package com.examanalyzer.backend;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record Credentials(String name, String email, String password) {}

    public record UserInfo(String name, String email) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody Credentials c,
                                           HttpServletRequest req, HttpServletResponse res) {
        String name = c.name() == null ? "" : c.name().trim();
        String email = c.email() == null ? "" : c.email().trim().toLowerCase();
        String password = c.password() == null ? "" : c.password();

        if (name.isEmpty() || !email.contains("@") || password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Enter your name, a valid email and a password of at least 8 characters."));
        }
        if (users.existsByEmail(email)) {
            return ResponseEntity.status(409).body(Map.of("error",
                    "An account with that email already exists."));
        }

        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        users.save(user);

        signIn(user, req, res);
        return ResponseEntity.ok(new UserInfo(user.getName(), user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Credentials c,
                                        HttpServletRequest req, HttpServletResponse res) {
        String email = c.email() == null ? "" : c.email().trim().toLowerCase();
        String password = c.password() == null ? "" : c.password();

        Optional<UserEntity> found = users.findByEmail(email);
        if (found.isEmpty() || !encoder.matches(password, found.get().getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Wrong email or password."));
        }

        signIn(found.get(), req, res);
        return ResponseEntity.ok(new UserInfo(found.get().getName(), found.get().getEmail()));
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me(Authentication auth) {
        Optional<UserEntity> user = users.findByEmail(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new UserInfo(user.get().getName(), user.get().getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    private void signIn(UserEntity user, HttpServletRequest req, HttpServletResponse res) {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, req, res);
    }
}