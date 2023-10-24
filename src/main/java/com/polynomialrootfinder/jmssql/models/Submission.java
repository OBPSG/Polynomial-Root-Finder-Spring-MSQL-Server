package com.polynomialrootfinder.jmssql.models;

public class Submission {
    int userId;
    Polynomial inputPolynomial;

    public Submission(int userid, Polynomial polynomial) {
        userId = userid;
        inputPolynomial = polynomial;
    };

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Polynomial getInputPolynomial() {
        return inputPolynomial;
    }

    public void setInputPolynomial(Polynomial inputPolynomial) {
        this.inputPolynomial = inputPolynomial;
    }
}
