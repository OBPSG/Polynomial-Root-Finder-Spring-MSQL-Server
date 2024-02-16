package com.polynomialrootfinder.jmssql.models;

public class RationalNumber {

    private Integer numerator;
    private Integer denominator;

    public RationalNumber(Integer n, Integer d){
        this.numerator = n;
        this.denominator = d;
        this.Reduce();
    }
    public Integer getNumerator() {
        return numerator;
    }

    public void setNumerator(Integer numerator) {
        this.numerator = numerator;
    }

    public Integer getDenominator() {
        return denominator;
    }

    public void setDenominator(Integer denominator) {
        this.denominator = denominator;
    }

    public double toDouble(){
        return  Double.valueOf(this.numerator)/Double.valueOf(this.denominator);
    }

    public void Reduce(){
        int gcd = GreatestCommonDivisor(this.numerator, this.denominator);
        if(gcd > 1) {
            this.numerator /= gcd;
            this.denominator /= gcd;
        }
    }


    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || o.getClass() != RationalNumber.class) return false;
        else
        {
            RationalNumber r2 = (RationalNumber) o;
            return ((this.getNumerator() == r2.getNumerator()) && (this.getDenominator() == r2.getDenominator()));
        }
    }

    public static Integer GreatestCommonDivisor (int num1, int num2) {
        int result = 1, i = 1;
        int a = Math.abs(num1), b = Math.abs(num2);
        while (i <= Math.min(a, b)){
            if(a % i == 0 && b % i == 0) result = i;
            i++;
        }
        return result;
    }
}
