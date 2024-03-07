package com.polynomialrootfinder.jmssql;

import com.polynomialrootfinder.jmssql.models.Polynomial;
import com.polynomialrootfinder.jmssql.msqldaos.SubmissionRepositoryDAO;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest(classes = {Polynomial.class})
public class PolynomialNormalizationTests {

    @Test
    void normalizedPolynomialShouldBeLeftAsIs(){
        Polynomial testPolynomial = new Polynomial(List.of(1, 2, 3, 4, 5), "z");
        testPolynomial.Normalize();

        Polynomial expectedPolynomial = new Polynomial();
        expectedPolynomial.degree = 4;
        expectedPolynomial.terms.add(new Polynomial.PolynomialTerm(1, "z", 4));
        expectedPolynomial.terms.add(new Polynomial.PolynomialTerm(2, "z", 3));
        expectedPolynomial.terms.add(new Polynomial.PolynomialTerm(3, "z", 2));
        expectedPolynomial.terms.add(new Polynomial.PolynomialTerm(4, "z", 1));
        expectedPolynomial.terms.add(new Polynomial.PolynomialTerm(5, null, 0));

        assert(testPolynomial.equals(expectedPolynomial));
    }

    @Test
    void PolynomialWithIncorrectDegreeFieldValueShouldBeFixed(){
        Polynomial expectedPolynomial = new Polynomial();
        expectedPolynomial.degree = 4;
        expectedPolynomial.terms = List.of(new Polynomial.PolynomialTerm(12, "t", 4),
                new Polynomial.PolynomialTerm(7, "t", 3),
                new Polynomial.PolynomialTerm(15, "t", 2),
                new Polynomial.PolynomialTerm(5, "t", 1),
                new Polynomial.PolynomialTerm(23, null, 0)
                );

        Polynomial testPolynomial = new Polynomial();
        testPolynomial.degree = 7;
        testPolynomial.terms.addAll(Arrays.asList(new Polynomial.PolynomialTerm(12, "t", 4),
                new Polynomial.PolynomialTerm(7, "t", 3),
                new Polynomial.PolynomialTerm(15, "t", 2),
                new Polynomial.PolynomialTerm(5, "t", 1),
                new Polynomial.PolynomialTerm(23, null, 0)));

        testPolynomial.Normalize();

        assert(testPolynomial.equals(expectedPolynomial));
    }

    @Test
    void PolynomialWithTermsOutOfOrderShouldBeSorted(){
        Polynomial expectedPolynomial = new Polynomial();
        expectedPolynomial.degree = 4;
        expectedPolynomial.terms = List.of(new Polynomial.PolynomialTerm(12, "t", 4),
                new Polynomial.PolynomialTerm(7, "t", 3),
                new Polynomial.PolynomialTerm(15, "t", 2),
                new Polynomial.PolynomialTerm(5, "t", 1),
                new Polynomial.PolynomialTerm(23, null, 0)
        );

        Polynomial testPolynomial = new Polynomial();
        testPolynomial.degree = 7;
        testPolynomial.terms.addAll(Arrays.asList(new Polynomial.PolynomialTerm(12, "t", 4),
                new Polynomial.PolynomialTerm(5, "t", 1),
                new Polynomial.PolynomialTerm(15, "t", 2),
                new Polynomial.PolynomialTerm(23, null, 0),
                new Polynomial.PolynomialTerm(7, "t", 3)
                ));

        testPolynomial.Normalize();

        assert(testPolynomial.equals(expectedPolynomial));

    }

    @Test
    void PolynomialWithMissingTermsShouldBePadded(){

        Polynomial expectedPolynomial = new Polynomial();
        expectedPolynomial.degree = 4;
        expectedPolynomial.terms = List.of(new Polynomial.PolynomialTerm(12, "t", 4),
                new Polynomial.PolynomialTerm(7, "t", 3),
                new Polynomial.PolynomialTerm(0, "t", 2),
                new Polynomial.PolynomialTerm(5, "t", 1),
                new Polynomial.PolynomialTerm(23, null, 0)
        );

        Polynomial testPolynomial = new Polynomial();
        testPolynomial.degree = 4;
        testPolynomial.terms.addAll(Arrays.asList(new Polynomial.PolynomialTerm(12, "t", 4),
                new Polynomial.PolynomialTerm(7, "t", 3),
                new Polynomial.PolynomialTerm(5, "t", 1),
                new Polynomial.PolynomialTerm(23, null, 0)
        ));

        testPolynomial.Normalize();

        assert(testPolynomial.equals(expectedPolynomial));

    }
}
