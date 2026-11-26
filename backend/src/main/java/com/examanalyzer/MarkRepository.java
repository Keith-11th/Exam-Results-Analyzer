package com.examanalyzer.backend;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkRepository extends JpaRepository<MarkEntity, Long> {

    List<MarkEntity> findByOwnerEmail(String ownerEmail);

    long countByOwnerEmail(String ownerEmail);

    Optional<MarkEntity> findByOwnerEmailAndLearnerIdAndSubjectIgnoreCase(
            String ownerEmail, String learnerId, String subject);
}