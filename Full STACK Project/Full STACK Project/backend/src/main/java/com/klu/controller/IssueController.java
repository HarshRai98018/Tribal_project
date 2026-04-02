package com.klu.controller;

import com.klu.dto.IssueRequest;
import com.klu.exception.ApiException;
import com.klu.model.Issue;
import com.klu.model.User;
import com.klu.service.IssueService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    public ResponseEntity<Issue> create(@RequestBody IssueRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.create(req));
    }

    @PatchMapping("/{id}/resolve")
    public Issue resolve(@PathVariable String id, @AuthenticationPrincipal User user) {
        if (!"admin".equals(user.getRole())) {
            throw ApiException.forbidden("Role access denied");
        }
        return issueService.resolve(id);
    }
}
