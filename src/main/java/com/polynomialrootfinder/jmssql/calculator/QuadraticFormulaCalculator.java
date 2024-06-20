package com.polynomialrootfinder.jmssql.calculator;

import com.polynomialrootfinder.jmssql.models.ComplexNumber;

import java.util.ArrayList;

;

public class QuadraticFormulaCalculator {
    public double a, b, c;

    public QuadraticFormulaCalculator(double a, double b, double c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public double getA() {
        return a;
    }

    public double getB() {
        return b;
    }

    public double getC() {
        return c;
    }

    public QuadraticFormulaSolutionPair Calculate() {
        QuadraticFormulaSolutionPair result = new QuadraticFormulaSolutionPair();
        double discriminant = this.b*this.b - 4*this.a*this.c;
        if(discriminant < 0) {
            result.solutionType = QuadraticFormulaSolutionType.TWO_COMPLEX;
            result.solutions.add(
                    new ComplexNumber((-1*this.b/(2*this.a)), Math.sqrt(Math.abs(discriminant))/(2*this.a))
            );
            result.solutions.add(
                    new ComplexNumber((-1*this.b/(2*this.a)), -1*Math.sqrt(Math.abs(discriminant))/(2*this.a))
            );
        }
        else if((discriminant - 0.0) <= 0.0001)
        {
            result.solutionType = QuadraticFormulaSolutionType.ONE_REAL;
            double solution = this.b*-1 / (2*this.a);
            result.solutions.add(new ComplexNumber(solution, 0.0));
        }
        else
        {
            result.solutionType = QuadraticFormulaSolutionType.TWO_REAL;
            result.solutions.add(
                    new ComplexNumber(((-1*this.b + Math.sqrt(discriminant) )/ (2*this.a)), 0.0)
            );
            result.solutions.add(
                    new ComplexNumber(((-1*this.b - Math.sqrt(discriminant) )/ (2*this.a)), 0.0)
            );
        }
        return result;
    }
}
