package com.examanalyzer.backend;

import java.util.Map;

public record LearnerResult(
        String learnerId,
        String name,
        String className,
        Map<String, Double> subjects,
        double total,
        double average,
        String grade,
        int position) {}