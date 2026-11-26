package com.examanalyzer.backend;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api")
public class ResultsController {

    private static final String PYTHON = "python";
    private static final String SCRIPT = "../analytics/analyze.py";

    private final ResultsService service;
    private final JsonMapper jsonMapper;

    public ResultsController(ResultsService service, JsonMapper jsonMapper) {
        this.service = service;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping("/marks")
    public ResponseEntity<Object> addMark(@RequestBody Mark mark, Authentication auth) {
        boolean invalid = mark.learnerId() == null || mark.learnerId().isBlank()
                || mark.name() == null || mark.name().isBlank()
                || mark.className() == null || mark.className().isBlank()
                || mark.subject() == null || mark.subject().isBlank()
                || mark.score() < 0 || mark.score() > 100;
        if (invalid) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "All fields are required and the score must be between 0 and 100."));
        }
        service.addMark(auth.getName(), mark);
        return ResponseEntity.ok(mark);
    }

    @PostMapping("/sample-data")
    public ResponseEntity<Void> sampleData(Authentication auth) {
        service.seed(auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/results")
    public List<LearnerResult> results(Authentication auth) {
        return service.computeResults(auth.getName());
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<LearnerResult> result(@PathVariable("id") String id, Authentication auth) {
        return service.findResult(auth.getName(), id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/analysis")
    public ResponseEntity<String> analysis(Authentication auth) {
        try {
            String json = jsonMapper.writeValueAsString(service.computeResults(auth.getName()));

            Process process = new ProcessBuilder(PYTHON, SCRIPT, "--stdin")
                    .redirectErrorStream(true)
                    .start();
            try (OutputStream os = process.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return ResponseEntity.status(500)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("Python analysis timed out.");
            }
            if (process.exitValue() != 0) {
                return ResponseEntity.status(500)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("Python analysis failed: " + output);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(output);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Could not run Python: " + e.getMessage());
        }
    }
}