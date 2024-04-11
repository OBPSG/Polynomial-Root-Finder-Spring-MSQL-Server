package com.polynomialrootfinder.jmssql.endpoints;

import com.polynomialrootfinder.jmssql.models.Polynomial;
import com.polynomialrootfinder.jmssql.models.Submission;
import com.polynomialrootfinder.jmssql.respositories.ISubmissionRepository;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SubmissionController {
    private final ISubmissionRepository repository;


    @Autowired
    public SubmissionController (ISubmissionRepository repository){
        this.repository = repository;
    }

    @GetMapping(value = "api/submissions/")
    Submission RetrieveById(@RequestParam String id) {
        Submission result = repository.findByID(Long.parseLong(id));
        if(result == null) throw new SubmissionNotFoundException(Long.parseLong(id));
        else return result;

    }

    @GetMapping(value = "api/submissions")
    List<Submission> retrieveRecent(){return repository.findRecent();}


    @PostMapping(value = "api/submissions")
    Submission createNew(@RequestBody Polynomial inputPolynomial) {
        return repository.save(inputPolynomial);
    }
}
