package com.polynomialrootfinder.jmssql.msqldaos;

import com.polynomialrootfinder.jmssql.calculator.QuadraticFormulaCalculator;
import com.polynomialrootfinder.jmssql.calculator.QuadraticFormulaSolutionType;
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
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
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



    //Helper function for saving a single polynomial
    //The Factoring Sequence Position parameter can be obtained from the polynomial's position in the parent submission's list,
    //While the Solution Factor for this polynomial can be extracted from the corresponding position in the Factored Zeroes list
    private Long polynomialSave(Polynomial polynomial,
                                   Long SubmissionId,
                                   Boolean isIntermediate,
                                   Integer FactoringSequencePosition,
                                   RationalNumber Solution) throws DataAccessException {
        //Insert polynomial first, then use returned key to insert the terms
        String polynomialInsertSQL = "INSERT INTO Polynomials (Degree, SubmissionId, IsIntermediate, FactoringSequenceNumber, " +
                "FactorNumerator, FactorDenominator) VALUES (?, ?, ?, ?, ?, ?)";
        String polynomialTermInsertSQL = "INSERT INTO PolynomialTerms (Coefficient, Variable, Exponent, PolynomialID) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbctemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(polynomialInsertSQL, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, polynomial.degree);
            ps.setLong(2, SubmissionId);
            ps.setBoolean(3, isIntermediate);
            if(isIntermediate){
                ps.setInt(4, FactoringSequencePosition);
                ps.setInt(5, Solution.getNumerator());
                ps.setInt(6, Solution.getDenominator());
            }
            else{
                ps.setNull(4, Types.INTEGER);
                ps.setNull(5, Types.INTEGER);
                ps.setNull(6, Types.INTEGER);
            }
            return ps;
        }, keyHolder);
        Long PolyId = keyHolder.getKey().longValue();

        jdbctemplate.batchUpdate(polynomialTermInsertSQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setDouble(1, polynomial.terms.get(i).getCoefficient());
                ps.setString(2, polynomial.terms.get(i).getVariable());
                ps.setInt(3, polynomial.terms.get(i).getExponent());
                ps.setLong(4, PolyId);
            }

            @Override
            public int getBatchSize() {
                return polynomial.terms.size();
            }
        });
        return PolyId;
    }

    @Override
    public Boolean save(Submission submission) throws DataAccessException {
        String submissionInsertSQL = "INSERT INTO Submissions (UserID, TimeSubmitted) VALUES (?,  ?)";
        String submissionUpdateSQL = "UPDATE Submissions SET InputPolynomialId = (?) WHERE Id = (?)";
        String submissionPRZInsertSql = "INSERT INTO PossibleRationalZeroes (Numerator, Denominator, SubmissionId) VALUES (?, ?, ?)";
        KeyHolder submissionKeyholder = new GeneratedKeyHolder();

        //Insert Input Polynomial then alter submission table entry to point to it
        jdbctemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(submissionInsertSQL, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, 0);
            ps.setDate(2, new Date(System.currentTimeMillis()));
            return ps;
            }
                , submissionKeyholder);
        Long submissionId = submissionKeyholder.getKey().longValue();
        Long InputPolyId = polynomialSave(submission.getInputPolynomial(),
                submissionId,
                false,
                null,
                null
                );
        jdbctemplate.update(submissionUpdateSQL, InputPolyId, submissionId);

        for(int i = 0; i < submission.IntermediatePolynomials.size(); i++)
        {
            polynomialSave(submission.IntermediatePolynomials.get(i),
                    submissionId,
                    true,
                    i + 1,
                    submission.FactoredZeroes.get(i)
                    );
        }

        jdbctemplate.batchUpdate(submissionPRZInsertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, submission.PossibleRationalZeroes.get(i).getNumerator());
                ps.setInt(2, submission.PossibleRationalZeroes.get(i).getDenominator());
                ps.setLong(3, submissionId);
            }
            @Override
            public int getBatchSize() {
                return submission.PossibleRationalZeroes.size();
            }
        });

    //Save the Quadratic Formula Solution pair, if it exists
        if(submission.QuadraticSolutionPair != null){
            if(submission.QuadraticSolutionPair.getSolutionType() == QuadraticFormulaSolutionType.TWO_REAL)
            {
                jdbctemplate.batchUpdate("INSERT INTO QuadraticFormulaSolutions (ComplexReal, SubmissionId) VALUES (? , ?)",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                ps.setDouble(1, submission.QuadraticSolutionPair.getSolutions().get(i).getReal());
                                ps.setLong(2, submissionId);
                            }

                            @Override
                            public int getBatchSize() {
                                return 2;
                            }
                        });
            }
            else if (submission.QuadraticSolutionPair.getSolutionType() == QuadraticFormulaSolutionType.TWO_COMPLEX) {
                jdbctemplate.batchUpdate("INSERT INTO QuadraticFormulaSolutions (ComplexReal, ComplexImaginary, SubmissionId) VALUES (?, ?, ?)",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                ps.setDouble(1, submission.QuadraticSolutionPair.getSolutions().get(i).getReal());
                                ps.setDouble(2, submission.QuadraticSolutionPair.getSolutions().get(i).getImaginary());
                                ps.setLong(3, submissionId);
                            }

                            @Override
                            public int getBatchSize() {
                                return 2;
                            }
                        });
            }
            else {
                jdbctemplate.update("INSERT INTO QuadraticForumulaSolutions (ComplexReal, SubmissionId) VALUES (?, ? )",
                        submission.QuadraticSolutionPair.getSolutions().get(0).getReal(), submissionId);
            }
        }

        logger.info("Saving of submission with ID = " + submissionId + " completed");
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
