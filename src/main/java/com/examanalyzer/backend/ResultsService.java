package com.examanalyzer.backend;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class ResultsService {

    private final List<Mark> marks = new CopyOnWriteArrayList<>();

    public ResultsService() {
        seed();
    }

    public void addMark(Mark mark) {
        // replace an existing mark for the same learner and subject
        marks.removeIf(m -> m.learnerId().equals(mark.learnerId())
                && m.subject().equalsIgnoreCase(mark.subject()));
        marks.add(mark);
    }

    public List<Mark> getAllMarks() {
        return List.copyOf(marks);
    }

    public static String gradeFor(double average) {
        if (average >= 80) return "A";
        if (average >= 70) return "B";
        if (average >= 60) return "C";
        if (average >= 50) return "D";
        if (average >= 40) return "E";
        return "F";
    }

    public List<LearnerResult> computeResults() {
        Map<String, List<Mark>> byLearner = marks.stream()
                .collect(Collectors.groupingBy(Mark::learnerId));

        List<LearnerResult> list = new ArrayList<>();
        for (List<Mark> learnerMarks : byLearner.values()) {
            Mark first = learnerMarks.get(0);
            Map<String, Double> subjects = new LinkedHashMap<>();
            double total = 0;
            for (Mark m : learnerMarks) {
                subjects.put(m.subject(), m.score());
                total += m.score();
            }
            double average = Math.round(total / learnerMarks.size() * 10.0) / 10.0;
            list.add(new LearnerResult(first.learnerId(), first.name(), first.className(),
                    subjects, total, average, gradeFor(average), 0));
        }

        list.sort(Comparator.comparingDouble(LearnerResult::total).reversed());

        // assign positions (learners with equal totals share a position)
        List<LearnerResult> ranked = new ArrayList<>();
        int position = 0;
        double previousTotal = -1;
        for (int i = 0; i < list.size(); i++) {
            LearnerResult r = list.get(i);
            if (r.total() != previousTotal) {
                position = i + 1;
                previousTotal = r.total();
            }
            ranked.add(new LearnerResult(r.learnerId(), r.name(), r.className(),
                    r.subjects(), r.total(), r.average(), r.grade(), position));
        }
        return ranked;
    }

    public Optional<LearnerResult> findResult(String learnerId) {
        return computeResults().stream()
                .filter(r -> r.learnerId().equals(learnerId))
                .findFirst();
    }

    private void seed() {
        String[] subjects = {"Math", "English", "Science"};
        Object[][] data = {
                {"L001", "Aisha Nakato", 85, 78, 90},
                {"L002", "Brian Okello", 62, 70, 55},
                {"L003", "Cynthia Namu", 45, 52, 38},
                {"L004", "David Mugisha", 91, 88, 94},
                {"L005", "Esther Auma", 70, 66, 72}
        };
        for (Object[] row : data) {
            for (int i = 0; i < subjects.length; i++) {
                marks.add(new Mark((String) row[0], (String) row[1], "Grade 7",
                        subjects[i], ((Integer) row[i + 2]).doubleValue()));
            }
        }
    }
}
