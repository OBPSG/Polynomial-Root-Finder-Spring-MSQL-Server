package com.polynomialrootfinder.jmssql;

import com.polynomialrootfinder.jmssql.calculator.SyntheticDivisionCalculator;
import com.polynomialrootfinder.jmssql.models.Polynomial;
import com.polynomialrootfinder.jmssql.models.RationalNumber;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

public class SyntheticDivisionCalculatorTests {
    @Test
    public void SDCTest1(){
        Polynomial inputPolynomial = new Polynomial(Arrays.asList(1, 3, -4, -12), "t");

        Polynomial expectedPolynomial = new Polynomial();
        expectedPolynomial.degree = 2;
        expectedPolynomial.terms.addAll(Arrays.asList(new Polynomial.PolynomialTerm(1, "t", 2),
                new Polynomial.PolynomialTerm(0, "t", 1),
                new Polynomial.PolynomialTerm(-4, null, 0)
                ));

        SyntheticDivisionCalculator calculator = new SyntheticDivisionCalculator();

        SyntheticDivisionCalculator.DivisionResultPair resultPair = calculator.testDivide(inputPolynomial, new RationalNumber( -3, 1));

        assert(resultPair.resultPolynomial.equals(expectedPolynomial));
        assert((resultPair.remainder - 0.0) <= 0.001);
    }

    @Test
    public void SDCTest2(){
        Polynomial inputPolynomial = new Polynomial(Arrays.asList(1, 1, -182, -288, 3744), "t");

        Polynomial expectedPolynomial = new Polynomial();
        expectedPolynomial.degree = 3;
        expectedPolynomial.terms.addAll(Arrays.asList(
                new Polynomial.PolynomialTerm(1, "t", 3),
                new Polynomial.PolynomialTerm(14, "t", 2),
                new Polynomial.PolynomialTerm(0, "t", 1),
                new Polynomial.PolynomialTerm(-288, null, 0)
        ));

        SyntheticDivisionCalculator calculator = new SyntheticDivisionCalculator();

        SyntheticDivisionCalculator.DivisionResultPair resultPair = calculator.testDivide(inputPolynomial, new RationalNumber( 13, 1));

        assert(resultPair.resultPolynomial.equals(expectedPolynomial));
        assert((resultPair.remainder - 0.0) <= 0.001);
    }



    @Test
    public void SDCTest3(){
        Polynomial inputPolynomial = new Polynomial(Arrays.asList(1, 1, -182, -288, 3744), "t");

        Polynomial expectedPolynomial = new Polynomial();
        expectedPolynomial.degree = 3;
        expectedPolynomial.terms.addAll(Arrays.asList(
                new Polynomial.PolynomialTerm(1, "t", 3),
                new Polynomial.PolynomialTerm(-7, "t", 2),
                new Polynomial.PolynomialTerm(-126, "t", 1),
                new Polynomial.PolynomialTerm(720, null, 0)
        ));

        SyntheticDivisionCalculator calculator = new SyntheticDivisionCalculator();

        SyntheticDivisionCalculator.DivisionResultPair resultPair = calculator.testDivide(inputPolynomial, new RationalNumber( -8, 1));

        assert(resultPair.resultPolynomial.equals(expectedPolynomial));
        assert((resultPair.remainder - (-2016)) <= 0.001);
    }

}
