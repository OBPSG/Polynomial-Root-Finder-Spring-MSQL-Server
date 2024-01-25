package com.polynomialrootfinder.jmssql.endpoints;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
class SubmissionNotFoundAdvice {

    @ResponseBody
    @ExceptionHandler(SubmissionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String SubmissionNotFoundHandler(SubmissionNotFoundException ex) {
        return ex.getMessage();
    }
}
