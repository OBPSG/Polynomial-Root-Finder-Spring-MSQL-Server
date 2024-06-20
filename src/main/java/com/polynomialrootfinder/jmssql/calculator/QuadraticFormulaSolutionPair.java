package com.polynomialrootfinder.jmssql.calculator;

import com.polynomialrootfinder.jmssql.models.ComplexNumber;

import java.util.ArrayList;

public class QuadraticFormulaSolutionPair{
    public QuadraticFormulaSolutionType solutionType;
    public ArrayList<ComplexNumber> solutions;

    public QuadraticFormulaSolutionPair(){
        this.solutions = new ArrayList<>();
    }

    public QuadraticFormulaSolutionType getSolutionType() {
        return solutionType;
    }

    public ArrayList<ComplexNumber> getSolutions() {
        return solutions;
    }
}