package com.polynomialrootfinder.jmssql.models;

public class ComplexNumber {
    double real;
    double imaginary;

    public ComplexNumber(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public double getReal() {
        return real;
    }

    public double getImaginary() {
        return imaginary;
    }

    @Override
    public String toString() {
        return this.getReal() + " + " + this.getImaginary() + "i";
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || o.getClass() != ComplexNumber.class) return false;
        else
        {
            ComplexNumber c2 = (ComplexNumber) o;
            return ((this.getReal() == c2.getReal()) && (this.getImaginary() == c2.getImaginary()));
        }
    }
}
