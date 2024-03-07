package com.polynomialrootfinder.jmssql.models;

import java.util.*;

public class Polynomial {
    public int degree;

    public List<PolynomialTerm> terms;

    public static class PolynomialTerm {
        public int coefficient;

        public String variable;

        public int exponent;

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

        public PolynomialTerm() {
        }
        public PolynomialTerm(int c, String v, int e) {
            this.coefficient = c;
            this.variable = v;
            this.exponent = e;
        }

        public boolean equals(Object o){
            if(this == o) return true;
            if(o == null || o.getClass() != PolynomialTerm.class) return false;
            else
            {
                PolynomialTerm t2 = (PolynomialTerm) o;
                return(this.getCoefficient() == t2.getCoefficient() && this.getVariable() == t2.getVariable() && this.getExponent() == t2.getExponent());
            }
        }

        public static Boolean areLike(PolynomialTerm a, PolynomialTerm b){
            return (a.exponent == b.exponent && a.variable == b.variable);
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
        this.terms = new ArrayList<>();
        for(Integer coefficient : coefficients){
            this.terms.add(new PolynomialTerm(coefficient, variable, exponent--));
        }
        terms.get(terms.size() - 1).variable = null;
    }

    //Housekeeping function that ensures the polynomial is of the form Ax^n + Bx^n-1 + ... + Yx + Z
    //Should be called before performing operations such as synthetic division on the polynomial
    public void Normalize(){
        //Set the degree field to be the same as the exponent of the highest exponent term
        this.degree = this.terms.stream().mapToInt(term -> term.getExponent()).max().getAsInt();

        //Combine like terms
        ListIterator<PolynomialTerm> outerIterator = this.terms.listIterator();

        while(outerIterator.hasNext()){
            PolynomialTerm term1 = outerIterator.next();
            ListIterator<PolynomialTerm> innerIterator = this.terms.listIterator(outerIterator.nextIndex());
            if(innerIterator.hasNext()) innerIterator.next();
            while(innerIterator.hasNext())
            {
                PolynomialTerm term2 = innerIterator.next();
                if(PolynomialTerm.areLike(term1, term2))
                {
                    term1.setCoefficient(term1.getCoefficient() + term2.getCoefficient());
                    terms.remove(term2);
                }
            }
        }

        //Make certain there is a term object for every exponent
        //If there isn't a term for an exponent, add one with a coefficient of zero and variable same as the leading term
        boolean[] hasTermForExponent = new boolean[this.degree + 1];

        for(PolynomialTerm term: this.terms){
            hasTermForExponent[term.getExponent()] = true;
        }

        String LeadingVariable = this.terms.stream().max(Comparator.comparingInt(PolynomialTerm::getExponent)).get().getVariable();
        for (int flagIndex = 0; flagIndex < hasTermForExponent.length; flagIndex++){
            if (!(hasTermForExponent[flagIndex])) this.terms.add(new PolynomialTerm(0, LeadingVariable, flagIndex));
        }

        //Order the terms in descending order by exponent
        this.terms.sort((term1, term2) -> term2.getExponent() - term1.getExponent());
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || o.getClass() != Polynomial.class) return false;
        else
        {
            Polynomial p2 = (Polynomial) o;
            if(this.degree != p2.degree || this.terms.size() != p2.terms.size()) return false;
            for(int i = 0; i < this.terms.size(); i++){
                if (!(this.terms.get(i).equals(p2.terms.get(i)))) return false;
            }
            return true;
        }
    }

    //Converts polynomial object to string of form ax^n +/- bx^n-1 +- ... +/- wx^2 +/- yx +/- z
    @Override
    public String toString() {
        String result = "";
        for(PolynomialTerm term : terms) {

            String sign;
            if(term.coefficient >= 0.0) sign = " + ";
            else sign = " - ";
            result = result.concat(sign + Math.abs(term.coefficient));
            if(term.exponent >= 2){
                result = result.concat(term.variable + "^" + term.exponent);
            }
            else if (term.exponent == 1)
            {
                result = result.concat(term.variable);
            }
        }
        return result.trim();
    }
    }
