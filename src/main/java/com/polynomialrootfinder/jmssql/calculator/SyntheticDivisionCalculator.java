package com.polynomialrootfinder.jmssql.calculator;

import com.polynomialrootfinder.jmssql.models.Polynomial;
import com.polynomialrootfinder.jmssql.models.RationalNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class SyntheticDivisionCalculator {

    public ArrayList<DivisionSequenceResultPair> DivideTestingAllZeroes (Polynomial initialPolynomial, List<RationalNumber> possibleZeroes){
        ArrayList<DivisionSequenceResultPair> result =  new ArrayList<>();
        Polynomial currentPolynomial = initialPolynomial;
        ListIterator<RationalNumber> iterator = possibleZeroes.listIterator();
        RationalNumber testZero = null;
        if(iterator.hasNext()) testZero = iterator.next();
        while( currentPolynomial.degree > 2){

            DivisionResultPair divisionTestResult = testDivide(currentPolynomial, testZero);
            if(Math.abs(divisionTestResult.remainder - 0.0) <= 0.0001){
                result.add(new DivisionSequenceResultPair(testZero, divisionTestResult.getResultPolynomial()));
                currentPolynomial = divisionTestResult.getResultPolynomial();
            }
            //Because a zero can have multiplicity, we must repeatedly check if it can be factored out
            //So only advance the iterator if the division remainder is not zero
            else {
                if(iterator.hasNext()) testZero = iterator.next();
                else break;
            }
        }
        return result;
    }

    public DivisionResultPair testDivide(Polynomial inputPolynomial, RationalNumber testZero){
        if(inputPolynomial.degree <= 2) {
            return null;
        }

        double carryDown = inputPolynomial.terms.get(0).getCoefficient();
        double divisor = testZero.toDouble();

        Polynomial resultPolynomial = new Polynomial();
        resultPolynomial.degree = inputPolynomial.degree - 1;
        for(int i = 1 ; i < inputPolynomial.terms.size(); i++){
            resultPolynomial.terms.add(new Polynomial.PolynomialTerm((int) carryDown, inputPolynomial.terms.get(i).getVariable(), inputPolynomial.degree - i));
            carryDown = carryDown*divisor + inputPolynomial.terms.get(i).getCoefficient();
        }
        Double remainder = carryDown;
        return new DivisionResultPair(resultPolynomial, remainder);

    }

    public class DivisionResultPair {

        public Polynomial resultPolynomial;
        public Double remainder;
        public DivisionResultPair(Polynomial resultPolynomial, Double remainder) {
            this.resultPolynomial = resultPolynomial;
            this.remainder = remainder;
        }

        public Polynomial getResultPolynomial() {
            return resultPolynomial;
        }

        public void setResultPolynomial(Polynomial resultPolynomial) {
            this.resultPolynomial = resultPolynomial;
        }

        public Double getRemainder() {
            return remainder;
        }

        public void setRemainder(Double remainder) {
            this.remainder = remainder;
        }

    }

    public class DivisionSequenceResultPair {
        public RationalNumber finalZero;
        public Polynomial reducedPolynomial;

        public DivisionSequenceResultPair(RationalNumber finalZero, Polynomial reducedPolynomial) {
            this.finalZero = finalZero;
            this.reducedPolynomial = reducedPolynomial;
        }

        public RationalNumber getFinalZero() {
            return finalZero;
        }

        public void setFinalZero(RationalNumber finalZero) {
            this.finalZero = finalZero;
        }

        public Polynomial getReducedPolynomial() {
            return reducedPolynomial;
        }

        public void setReducedPolynomial(Polynomial reducedPolynomial) {
            this.reducedPolynomial = reducedPolynomial;
        }
    }
}


