package com.examanalyzer.backend;

import jakarta.persistence.*;

@Entity
@Table(name = "marks")
public class MarkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerEmail;
    private String learnerId;
    private String name;
    private String className;
    private String subject;
    private double score;

    public MarkEntity() {}

    public Long getId() { return id; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public String getLearnerId() { return learnerId; }
    public void setLearnerId(String learnerId) { this.learnerId = learnerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}