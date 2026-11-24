package com.examanalyzer.backend;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ResultsController {

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
}