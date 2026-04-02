package com.klu.service;

import com.klu.dto.IssueRequest;
import com.klu.exception.ApiException;
import com.klu.model.Issue;
import com.klu.repository.IssueRepository;
import org.springframework.stereotype.Service;

@Service
public class IssueService {

    private final IssueRepository issueRepo;

    public IssueService(IssueRepository issueRepo) {
        this.issueRepo = issueRepo;
    }

    public Issue create(IssueRequest req) {
        if (req.getType() == null || req.getMessage() == null) {
            throw ApiException.badRequest("Missing issue details");
        }

        Issue issue = Issue.builder()
                .id("I" + System.currentTimeMillis() % 1000000)
                .type(req.getType())
                .message(req.getMessage())
                .status("open")
                .build();

        return issueRepo.save(issue);
    }

    public Issue resolve(String id) {
        Issue issue = issueRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Issue not found"));
        issue.setStatus("resolved");
        return issueRepo.save(issue);
    }
}
