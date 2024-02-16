package com.polynomialrootfinder.jmssql.calculator;

import com.polynomialrootfinder.jmssql.models.Polynomial;
import com.polynomialrootfinder.jmssql.models.RationalNumber;
import com.polynomialrootfinder.jmssql.msqldaos.SubmissionRepositoryDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RationalZeroTheoremCalculator {

    private static final Logger logger = LoggerFactory.getLogger(RationalZeroTheoremCalculator.class);

    Polynomial polynomial;

    public RationalZeroTheoremCalculator(Polynomial inputPolynomial) {
        this.polynomial = inputPolynomial;

    }

    public List<RationalNumber> FindAllPossibleZeroes() {
        List<RationalNumber> result = new ArrayList<>();
        List<Integer> pFactors = GetAllFactors(polynomial.terms.get(0).getCoefficient());
        List<Integer> qFactors = GetAllFactors(polynomial.terms.get(polynomial.terms.size() - 1).getCoefficient());
        for(Integer pFactor: pFactors){
            for(Integer qFactor: qFactors){
                RationalNumber newPRZ = new RationalNumber(qFactor, pFactor);
                if(!(result.contains(newPRZ))) result.add(newPRZ);
                newPRZ = new RationalNumber(qFactor * -1, pFactor);
                if(!(result.contains(newPRZ))) result.add(newPRZ);
            }
        }
        return result;
    }

    public static List<Integer> GetAllFactors(int inputNum) {
        int n = Math.abs(inputNum);
        List<Integer> result = new ArrayList<>();
        for(int i = 1; i <= n; i++)
        {
            if(n % i == 0) result.add(i);
        }
        return result;
    }
}

