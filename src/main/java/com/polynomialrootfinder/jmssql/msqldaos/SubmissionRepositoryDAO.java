package com.polynomialrootfinder.jmssql.msqldaos;

import com.polynomialrootfinder.jmssql.calculator.QuadraticFormulaCalculator;
import com.polynomialrootfinder.jmssql.calculator.RationalZeroTheoremCalculator;
import com.polynomialrootfinder.jmssql.calculator.SyntheticDivisionCalculator;
import com.polynomialrootfinder.jmssql.models.Polynomial;
import com.polynomialrootfinder.jmssql.models.RationalNumber;
import com.polynomialrootfinder.jmssql.models.Submission;
import com.polynomialrootfinder.jmssql.respositories.ISubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class SubmissionRepositoryDAO implements ISubmissionRepository {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionRepositoryDAO.class);

    public class PolynomialTermMapper implements RowMapper<Polynomial.PolynomialTerm> {
        @Override
        public Polynomial.PolynomialTerm mapRow(ResultSet rs, int rowNum) throws SQLException {
            Polynomial.PolynomialTerm newTerm = new Polynomial.PolynomialTerm();
            newTerm.setCoefficient(rs.getInt("Coefficient"));
            newTerm.setExponent(rs.getInt("Exponent"));
            newTerm.setVariable(rs.getString("Variable"));
            return newTerm;
        }
    }

    public class PolynomialRSE implements ResultSetExtractor<Polynomial> {

        @Override
        public Polynomial extractData(ResultSet rs) throws SQLException, DataAccessException {
            return null;
        }
    }

    public class RationalNumberMapper implements RowMapper<RationalNumber> {
        @Override
        public RationalNumber mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RationalNumber(rs.getInt("Numerator"), rs.getInt("Denominator"));
        }
    }


    @Autowired
    private JdbcTemplate jdbctemplate;

    @Override
    public Boolean save(Submission submission) throws DataAccessException {
        jdbctemplate.update("INSERT INTO Polynomials (Degree) VALUES (?)", submission.getInputPolynomial().degree);
        //Get ID of record that was just inserted to use as foreign key
        //TODO(?) Create Helper Classes for getting SQL Identity Values (among other common tasks) Or use ExecuteAndReturnKey?

        Long PolyID = jdbctemplate.query("SELECT IDENT_CURRENT('Polynomials')", (ResultSetExtractor<Long>) rs -> {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return null;
        });

       jdbctemplate.batchUpdate("INSERT INTO PolynomialTerms (Coefficient, Variable, Exponent, PolynomialID) VALUES (?, ?, ?, ?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setDouble(1, submission.getInputPolynomial().terms.get(i).getCoefficient());
                ps.setString(2, submission.getInputPolynomial().terms.get(i).getVariable());
                ps.setInt(3, submission.getInputPolynomial().terms.get(i).getExponent());
                ps.setLong(4, PolyID);
            }

            @Override
            public int getBatchSize() {
                return submission.getInputPolynomial().terms.size();
            }
        });

        jdbctemplate.batchUpdate("INSERT INTO PossibleRationalZeroes (Numerator, Denominator, PolynomialId) VALUES (?, ?, ?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, submission.PossibleRationalZeroes.get(i).getNumerator());
                ps.setInt(2, submission.PossibleRationalZeroes.get(i).getDenominator());
                ps.setLong(3, PolyID);
            }
            @Override
            public int getBatchSize() {
                return submission.PossibleRationalZeroes.size();
            }
        });

        jdbctemplate.update("INSERT INTO Submissions (UserID, InputPolynomialId, TimeSubmitted) VALUES (?, ?, ?)", (Object) 0, PolyID, new Date(System.currentTimeMillis()));
        logger.info("Saving of polynomial with ID = " + PolyID + " completed");
        return true;
    }

    @Override
    public Submission findByID(long id) {
        String SubmissionQuery = "SELECT Submissions.Id, Submissions.UserID, Submissions.TimeSubmitted, Polynomials.Id as PolyId, Polynomials.degree, PolynomialTerms.Coefficient, PolynomialTerms.Variable, PolynomialTerms.Exponent FROM Submissions\n" +
                "JOIN Polynomials ON Submissions.InputPolynomialId = Polynomials.Id\n" +
                "Join PolynomialTerms on Polynomials.Id = PolynomialTerms.PolynomialID\n" +
                "WHERE Submissions.id = ?";
        Submission submission = jdbctemplate.query(SubmissionQuery, new Object[]{(Object) id}, new ResultSetExtractor<Submission>() {
            @Override
            public Submission extractData(ResultSet rs) throws SQLException, DataAccessException {
                Polynomial polynomial = new Polynomial();
                int userId = 0;
                Long submissionId = null;
                Date timeSubmitted = null;
                while (rs.next()) {
                    if (rs.getRow() == 1) {
                        userId = rs.getInt("UserID");
                        timeSubmitted = rs.getDate("TimeSubmitted");
                        polynomial.degree = rs.getInt("degree");
                        submissionId = rs.getLong("Id");
                    }
                    polynomial.terms.add(new PolynomialTermMapper().mapRow(rs, rs.getRow()));
                }
                if (submissionId == null) {
                    return null;
                }
                Submission submission = new Submission(userId, polynomial);
                submission.timeSubmitted = timeSubmitted;
                submission.setId(submissionId);
                return submission;
            }
        });

        SubmissionQuery = "SELECT * FROM Submissions \n" +
                "JOIN Polynomials on Submissions.InputPolynomialId = Polynomials.Id\n" +
                "Join PossibleRationalZeroes on Submissions.InputPolynomialId = PossibleRationalZeroes.PolynomialId\n" +
                "Where Submissions.id = ?";

        submission.PossibleRationalZeroes = jdbctemplate.query(SubmissionQuery, new Object[]{(Object) id}, new RationalNumberMapper());

        return submission;
    }

    @Override
    public List<Submission> findByUser(long userId) {
        return null;
    }

    @Override
    public List<Submission> findRecent() {
        String SubmissionsQuery = "SELECT Submissions.Id, Submissions.UserID, Submissions.TimeSubmitted, Polynomials.Id as PolyId, Polynomials.degree, PolynomialTerms.Coefficient, PolynomialTerms.Variable, PolynomialTerms.Exponent FROM Submissions\n" +
                "JOIN Polynomials ON Submissions.InputPolynomialId = Polynomials.Id\n" +
                "Join PolynomialTerms on Polynomials.Id = PolynomialTerms.PolynomialID\n" +
                "WHERE Submissions.id IN (SELECT TOP 100 id from Submissions)\n" +
                "ORDER BY Submissions.TimeSubmitted DESC;";
        List<Submission> submissionList = jdbctemplate.query(SubmissionsQuery, new ResultSetExtractor<List<Submission>>() {
            @Override
            public List<Submission> extractData(ResultSet rs) throws SQLException, DataAccessException {
                List<Submission> SubmissionList = new ArrayList<>();
                long submissionId = -1;
                long currentId = -1;
                Submission currentSubmission = null;
                Polynomial newPolynomial = null;
                Timestamp timestamp = null;
                while (rs.next()) {
                    //Special actions for beginning of result set
                    if(currentSubmission == null)
                    {
                        currentId = rs.getLong("Id");
                        currentSubmission = new Submission(0, new Polynomial());
                        currentSubmission.setId(currentId);
                        currentSubmission.setTimeSubmitted(rs.getTimestamp("TimeSubmitted"));
                        newPolynomial = new Polynomial();
                        newPolynomial.degree = rs.getInt("degree");
                    }
                    //Check if we've reached the end of the subset for the current submission
                    if(currentId != rs.getLong("Id")){
                        currentSubmission.setInputPolynomial(newPolynomial);
                        SubmissionList.add(currentSubmission);
                        currentId = rs.getLong("Id");
                        currentSubmission = new Submission(0, new Polynomial());
                        currentSubmission.setId(currentId);
                        currentSubmission.setTimeSubmitted(rs.getTimestamp("TimeSubmitted"));
                        newPolynomial = new Polynomial();
                        newPolynomial.degree = rs.getInt("degree");
                    }
                    newPolynomial.terms.add( new PolynomialTermMapper().mapRow(rs, rs.getRow()));
                }
                currentSubmission = new Submission(0, newPolynomial);
                currentSubmission.setId(currentId);
                currentSubmission.timeSubmitted = timestamp;
                SubmissionList.add(currentSubmission);
                return SubmissionList;
            }
        });


        SubmissionsQuery = "SELECT Submissions.id, Polynomials.Id AS PolyID, Numerator, Denominator FROM Submissions \n" +
                "JOIN Polynomials on Submissions.InputPolynomialId = Polynomials.Id\n" +
                "Join PossibleRationalZeroes on Submissions.InputPolynomialId = PossibleRationalZeroes.PolynomialId\n" +
                "Where Submissions.id IN (SELECT TOP 100 id from Submissions)\n" +
                "ORDER BY Submissions.TimeSubmitted DESC;";

        List<PRZListSubmissionPair> PRZBigList = jdbctemplate.query(SubmissionsQuery, new ResultSetExtractor<List<PRZListSubmissionPair>>() {
            @Override
            public List<PRZListSubmissionPair> extractData(ResultSet rs) throws SQLException, DataAccessException {
                List<PRZListSubmissionPair> BigList = new ArrayList<>();
                while (rs.next()) {
                    PRZListSubmissionPair newPair = new PRZListSubmissionPair(rs.getLong("id"),
                            new RationalNumber(rs.getInt("Numerator"), rs.getInt("Denominator")));
                    BigList.add(newPair);
                }
                return BigList;
            }
        });

        for (Submission submission : submissionList) {
            for(PRZListSubmissionPair pair: PRZBigList){
                if(submission.getId() == pair.SubmissionId())
                {
                    submission.PossibleRationalZeroes.add(pair.rn());
                }
            }
        }

        return submissionList;
    }

    @Override
    public int UpdateSubmission(Polynomial newPolynomial) {
        return 0;
    }

    @Override
    public int DeleteSubmission(long submissionToDeleteId) {
        return 0;
    }

}

record PRZListSubmissionPair(Long SubmissionId, RationalNumber rn){};
