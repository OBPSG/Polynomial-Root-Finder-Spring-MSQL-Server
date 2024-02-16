package com.polynomialrootfinder.jmssql.models;

import java.util.ArrayList;
import java.util.List;

public class Polynomial {
    public int degree;

    public List<PolynomialTerm> terms;

    public static class PolynomialTerm {
        public int coefficient;

        public String variable;

        public int getCoefficient() {
            return coefficient;
        }

        public void setCoefficient(int coefficient) {
            this.coefficient = coefficient;
        }

        public String getVariable() {
            return variable;
        }

        public void setVariable(String variable) {
            this.variable = variable;
        }

        public int getExponent() {
            return exponent;
        }

        public void setExponent(int exponent) {
            this.exponent = exponent;
        }

        public int exponent;

        public PolynomialTerm() {
        }
        public PolynomialTerm(int c, String v, int e) {
            this.coefficient = c;
            this.variable = v;
            this.exponent = e;
        }
    }

    public Polynomial() {
        this.terms = new ArrayList<>();
    }

    //Constructs a new Polynomial from a list of coefficients. They are assumed to be in descending order of exponent
    //i.e. of the form ax^3 + bx^2 + cx + d, and zeroes should be included for terms of a power with no coefficient
    public Polynomial(List<Integer> coefficients, String variable) {
        this.degree = coefficients.size() - 1;
        int exponent = this.degree;
        for(Integer coefficient : coefficients){
            this.terms.add(new PolynomialTerm(coefficient, variable, exponent--));
        }
        terms.get(terms.size() - 1).variable = null;
    }


    //Converts polynomial object to string of form ax^n +/- bx^n-1 +- ... +/- wx^2 +/- yx +/- z
    public String toString() {
        String result = "";
        for(PolynomialTerm term : terms) {

            String sign;
            if(term.coefficient >= 0.0) sign = " + ";
            else sign = " - ";
            result.concat(sign + Math.abs(term.coefficient));
            if(term.exponent >= 2){
                result.concat(term.variable + term.exponent);
            }
            else if (term.exponent == 1)
            {
                result.concat(term.variable);
            }
        }
        return result.trim();
    }
    }
