package com.polynomialrootfinder.jmssql.endpoints;

import com.polynomialrootfinder.jmssql.models.Submission;
import com.polynomialrootfinder.jmssql.respositories.ISubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubmissionController {
    private final ISubmissionRepository repository;


    @Autowired
    public SubmissionController (ISubmissionRepository repository){
        this.repository = repository;
    }

    @PostMapping(value = "/submissions")
    int createNew(@RequestBody Submission newSubmission) {
        return repository.save(newSubmission);
    }
}
