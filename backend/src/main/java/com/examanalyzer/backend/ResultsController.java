package com.examanalyzer.backend;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ResultsController {

    // use "py" here if only "py --version" worked for you
    private static final String PYTHON = "python";
    private static final String SCRIPT = "../analytics/analyze.py";

    private final ResultsService service;

    public ResultsController(ResultsService service) {
        this.service = service;
    }

    @PostMapping("/marks")
    public Mark addMark(@RequestBody Mark mark) {
        service.addMark(mark);
        return mark;
    }

    @GetMapping("/results")
    public List<LearnerResult> results() {
        return service.computeResults();
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<LearnerResult> result(@PathVariable("id") String id) {
        return service.findResult(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/analysis")
    public ResponseEntity<String> analysis() {
        try {
            Process process = new ProcessBuilder(PYTHON, SCRIPT)
                    .redirectErrorStream(true)
                    .start();
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