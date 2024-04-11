package com.polynomialrootfinder.jmssql.respositories;

import com.polynomialrootfinder.jmssql.models.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ISubmissionRepository{
    // URL_____________________|_HTTP Method_|_Action
    //
    // api/submissions_________|_POST________|_Create A new submission
    Submission save(Polynomial inputPolynomial);

    // api/submissions/:id?____|_GET_________|_Find a submission by ID
    Submission findByID(long id);

    // api/submissions/:userId_|_GET_________|_Find all submissions by user ID
    List<Submission> findByUser(long userId);

    // api/submissions_________|_GET_________|_Find the most recent submissions, up to 100
    List<Submission> findRecent();

    // api/submissions/:id_____|_PUT_________|_Amend a submitted polynomial, recomputing it as appropriate
    int UpdateSubmission(Polynomial newPolynomial);

    // api/submissions/:id_____|_DELETE______|_Delete a submission by ID
    int DeleteSubmission(long submissionToDeleteId);
}
