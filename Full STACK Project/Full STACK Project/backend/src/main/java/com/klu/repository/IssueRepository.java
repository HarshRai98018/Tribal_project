package com.klu.repository;

import com.klu.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, String> {
    long countByStatus(String status);
}
