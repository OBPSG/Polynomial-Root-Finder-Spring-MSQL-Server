package com.polynomialrootfinder.jmssql.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Submission {

    public Date getTimeSubmitted() {
        return timeSubmitted;
    }

    public void setTimeSubmitted(Date timeSubmitted) {
        this.timeSubmitted = timeSubmitted;
    }

    public Date timeSubmitted;
    public long getId() {
        return Id;
    }

    public void setId(long id) {
        Id = id;
    }

    long Id;
    long userId;
    Polynomial inputPolynomial;

    public Submission(int userid, Polynomial polynomial) {
        userId = userid;
        inputPolynomial = polynomial;
        PossibleRationalZeroes = new ArrayList<>();
    };

    public List<RationalNumber> PossibleRationalZeroes;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Polynomial getInputPolynomial() {
        return inputPolynomial;
    }

    public void setInputPolynomial(Polynomial inputPolynomial) {
        this.inputPolynomial = inputPolynomial;
    }
}
