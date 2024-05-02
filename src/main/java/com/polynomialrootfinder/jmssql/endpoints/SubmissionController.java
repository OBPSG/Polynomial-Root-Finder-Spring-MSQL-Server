package com.polynomialrootfinder.jmssql.endpoints;

import com.polynomialrootfinder.jmssql.calculator.QuadraticFormulaCalculator;
import com.polynomialrootfinder.jmssql.calculator.RationalZeroTheoremCalculator;
import com.polynomialrootfinder.jmssql.calculator.SyntheticDivisionCalculator;
import com.polynomialrootfinder.jmssql.models.Polynomial;
import com.polynomialrootfinder.jmssql.models.RationalNumber;
import com.polynomialrootfinder.jmssql.models.Submission;
import com.polynomialrootfinder.jmssql.respositories.ISubmissionRepository;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class SubmissionController {
    private final ISubmissionRepository repository;


    @Autowired
    public SubmissionController (ISubmissionRepository repository){
        this.repository = repository;
    }

    @GetMapping(value = "api/submissions/")
    Submission RetrieveById(@RequestParam String id) {
        Submission result = repository.findByID(Long.parseLong(id));
        if(result == null) throw new SubmissionNotFoundException(Long.parseLong(id));
        else return result;

    }

    @GetMapping(value = "api/submissions")
    List<Submission> retrieveRecent(){return repository.findRecent();}


    @PostMapping(value = "api/submissions")
    Submission createNew(@RequestBody Polynomial inputPolynomial) {
        Polynomial normalizedPolynomial = inputPolynomial;
        normalizedPolynomial.Normalize();

        Submission result = new Submission(0, normalizedPolynomial);
        result.setTimeSubmitted(new Date());

        RationalZeroTheoremCalculator RZTCalculator = new RationalZeroTheoremCalculator(normalizedPolynomial);
        List<RationalNumber> PRZs = RZTCalculator.FindAllPossibleZeroes();
        result.PossibleRationalZeroes = PRZs;

        SyntheticDivisionCalculator SDCalculator =  new SyntheticDivisionCalculator();
        ArrayList<SyntheticDivisionCalculator.DivisionSequenceResultPair> DivisionResults = SDCalculator.DivideTestingAllZeroes(normalizedPolynomial, PRZs);
        for(SyntheticDivisionCalculator.DivisionSequenceResultPair DivisionResult: DivisionResults) {
            result.FactoredZeroes.add(DivisionResult.finalZero);
            result.IntermediatePolynomials.add(DivisionResult.reducedPolynomial);
        }

        Polynomial finalPolynomial = result.IntermediatePolynomials.get(result.IntermediatePolynomials.size() - 1);

        if (finalPolynomial.degree == 2)
        {
            QuadraticFormulaCalculator calculator = new QuadraticFormulaCalculator(
                    finalPolynomial.terms.get(0).getCoefficient(),
                    finalPolynomial.terms.get(1).getCoefficient(),
                    finalPolynomial.terms.get(2).getCoefficient()
            );

            result.QuadraticSolutionPair = calculator.Calculate();
        }

        repository.save(result);
        return result;
    }
}
