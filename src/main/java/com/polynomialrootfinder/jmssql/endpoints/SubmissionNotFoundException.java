package com.polynomialrootfinder.jmssql.endpoints;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


public class SubmissionNotFoundException extends RuntimeException {

    SubmissionNotFoundException(Long id) {
        super("Submission of ID " + id + " does not exist");
    }
}
