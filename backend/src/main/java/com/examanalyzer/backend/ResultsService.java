package com.examanalyzer.backend;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class ResultsService implements CommandLineRunner {

    public static final String DEMO_OWNER = "demo";

    private final MarkRepository repo;

    public ResultsService(MarkRepository repo) {
        this.repo = repo;
    }

    // runs once at startup: add sample data only if the demo owner has none
    @Override
    public void run(String... args) {
        if (repo.countByOwnerEmail(DEMO_OWNER) == 0) {
            seed();
        }
    }

    public void addMark(String owner, Mark mark) {
        MarkEntity e = repo
                .findByOwnerEmailAndLearnerIdAndSubjectIgnoreCase(owner, mark.learnerId(), mark.subject())
                .orElseGet(MarkEntity::new);
        e.setOwnerEmail(owner);
        e.setLearnerId(mark.learnerId());
        e.setName(mark.name());
        e.setClassName(mark.className());
        e.setSubject(mark.subject());
        e.setScore(mark.score());
        repo.save(e);
    }

    public static String gradeFor(double average) {
        if (average >= 80) return "A";
        if (average >= 70) return "B";
        if (average >= 60) return "C";
        if (average >= 50) return "D";
        if (average >= 40) return "E";
        return "F";
    }

    public List<LearnerResult> computeResults(String owner) {
        List<Mark> marks = repo.findByOwnerEmail(owner).stream()
                .map(e -> new Mark(e.getLearnerId(), e.getName(), e.getClassName(),
                        e.getSubject(), e.getScore()))
                .toList();

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

    public Optional<LearnerResult> findResult(String owner, String learnerId) {
        return computeResults(owner).stream()
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
                addMark(DEMO_OWNER, new Mark((String) row[0], (String) row[1], "Grade 7",
                        subjects[i], ((Integer) row[i + 2]).doubleValue()));
            }
        }
    }
}