package com.polynomialrootfinder.jmssql.msqldaos;

import com.polynomialrootfinder.jmssql.calculator.*;
import com.polynomialrootfinder.jmssql.models.ComplexNumber;
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
                jdbctemplate.update("INSERT INTO QuadraticFormulaSolutions (ComplexReal, SubmissionId) VALUES (?, ? )",
                        submission.QuadraticSolutionPair.getSolutions().get(0).getReal(), submissionId);
            }
        }

        logger.info("Saving of submission with ID = " + submissionId + " completed");
        return true;
    }

    @Override
    public Submission findByID(long id) {

        Submission submission = jdbctemplate.query("SELECT * FROM Submissions WHERE id = ?",  new Object[]{id}, new ResultSetExtractor<Submission>() {
            @Override
            public Submission extractData(ResultSet rs) throws SQLException, DataAccessException {
                Submission newSubmission = new Submission();
                rs.next();
                newSubmission.setId(rs.getLong("Id"));
                newSubmission.setUserId(rs.getLong("UserID"));
                newSubmission.setTimeSubmitted(rs.getDate("TimeSubmitted"));

                return newSubmission;
            }
        });

        submission.setInputPolynomial(jdbctemplate.query("SELECT * from Submissions JOIN Polynomials on Submissions.Id = Polynomials.SubmissionId " +
                "JOIN PolynomialTerms ON Polynomials.Id = PolynomialTerms.PolynomialID " +
                "WHERE Submissions.Id = ? AND Polynomials.IsIntermediate = 0;",
                new Object[]{id},
                new ResultSetExtractor<Polynomial>() {
                    @Override
                    public Polynomial extractData(ResultSet rs) throws SQLException, DataAccessException {
                        Polynomial newPolynomial = new Polynomial();
                        while(rs.next()){
                            if(rs.getRow() == 1)
                            {
                                newPolynomial.degree = rs.getInt("Degree");
                            }
                            newPolynomial.terms.add(new PolynomialTermMapper().mapRow(rs, rs.getRow()));
                        }
                        return newPolynomial;
                    }
                }
        ));

        String PRZListQuery = "SELECT * FROM Submissions \n" +
                "Join PossibleRationalZeroes on Submissions.id = PossibleRationalZeroes.SubmissionId\n" +
                "Where Submissions.id = ?";

        submission.PossibleRationalZeroes = jdbctemplate.query(PRZListQuery, new Object[]{id}, new RationalNumberMapper());

        String IntermediatePolynomialsQuery = "SELECT Polynomials.Id as PID, Polynomials.Degree AS Degree, Polynomials.FactoringSequenceNumber AS FSNO, PolynomialTerms.Coefficient AS Coefficient, PolynomialTerms.Variable AS Variable, PolynomialTerms.Exponent AS Exponent  \n" +
                "FROM Submissions JOIN Polynomials ON Submissions.Id = Polynomials.SubmissionId\n" +
                "JOIN PolynomialTerms ON Polynomials.Id = PolynomialTerms.PolynomialID\n" +
                "WHERE Submissions.Id = ? AND Polynomials.IsIntermediate = 1\n" +
                "ORDER BY Polynomials.FactoringSequenceNumber;";

        submission.IntermediatePolynomials = jdbctemplate.query(IntermediatePolynomialsQuery, new Object[]{id}, new ResultSetExtractor<>() {
            @Override
            public ArrayList<Polynomial> extractData(ResultSet rs) throws SQLException, DataAccessException {
                ArrayList polynomials = new ArrayList<Polynomial>();
                long polynomialId = -1;
                Polynomial currentPolynomial = null;
                while(rs.next())
                {
                    if(rs.getLong("PID") != polynomialId)
                    {
                        if(currentPolynomial != null) {polynomials.add(currentPolynomial);}
                        polynomialId = rs.getLong("PID");
                        currentPolynomial = new Polynomial();
                        currentPolynomial.degree = rs.getInt("Degree");
                    }
                    currentPolynomial.terms.add(new PolynomialTermMapper().mapRow(rs, rs.getRow()));
                }
                polynomials.add(currentPolynomial);
                return polynomials;
            }
        });

        String FactorsQuery = "SELECT * FROM Submissions JOIN Polynomials ON Submissions.Id = Polynomials.SubmissionId\n" +
                "WHERE Submissions.Id = ? ORDER BY Polynomials.FactoringSequenceNumber;";
        submission.FactoredZeroes = jdbctemplate.query(FactorsQuery, new Object[]{id}, new ResultSetExtractor<ArrayList<RationalNumber>>() {
            @Override
            public ArrayList<RationalNumber> extractData(ResultSet rs) throws SQLException, DataAccessException {
                ArrayList<RationalNumber> result = new ArrayList<>();
                while(rs.next())
                {
                    if(rs.getInt("FactoringSequenceNumber") != 0)
                    {
                        result.add(new RationalNumber(rs.getInt("FactorNumerator"), rs.getInt("FactorDenominator")));
                    }
                }
                return result;
            }
        });

        String QuadraticFormulaSolutionQuery = "SELECT * FROM Submissions JOIN QuadraticFormulaSolutions ON Submissions.Id = QuadraticFormulaSolutions.SubmissionId\n" +
                "WHERE Submissions.Id = ?";
        submission.QuadraticSolutionPair = jdbctemplate.query(QuadraticFormulaSolutionQuery, new Object[]{id}, new ResultSetExtractor<QuadraticFormulaSolutionPair>() {
            @Override
            public QuadraticFormulaSolutionPair extractData(ResultSet rs) throws SQLException, DataAccessException {
                QuadraticFormulaSolutionPair result = new QuadraticFormulaSolutionPair();
                //Check size of result set to determine the type of solution

                while(rs.next())
                {
                    if(rs.getDouble("ComplexImaginary") == 0)
                    {
                        result.solutions.add(new ComplexNumber(rs.getDouble("ComplexReal"), 0.0));
                    }
                    else
                    {
                        result.solutions.add(new ComplexNumber(rs.getDouble("ComplexReal"), rs.getDouble("ComplexImaginary")));
                    }
                }
                if(result.solutions.size() == 0)
                {
                    return null;
                }
                else if (result.solutions.size() == 1)
                {
                    result.solutionType =  QuadraticFormulaSolutionType.ONE_REAL;
                }
                else {
                    if (result.solutions.get(0).getImaginary() != 0.0)
                    {
                        result.solutionType = QuadraticFormulaSolutionType.TWO_COMPLEX;
                    }
                    else
                    {
                        result.solutionType = QuadraticFormulaSolutionType.TWO_REAL;
                    }
                }
                return result;
            }
        });

        return submission;
    }

    @Override
    public List<Submission> findByUser(long userId) {
        return null;
    }

    @Override
    public List<Submission> findRecent() {
        List<Submission> submissionList = new ArrayList<>();
        String SubmissionsQuery = "SELECT Id FROM Submissions\n" +
                "ORDER BY Submissions.TimeSubmitted DESC;";

        List<Long> submissionKeys =  jdbctemplate.query(SubmissionsQuery, new RowMapper<Long>() {
            @Override
            public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                return (Long) rs.getLong("Id");
            }
        });

        for (long key: submissionKeys){
            submissionList.add(findByID(key));
        }

        return submissionList;
    }

    @Override
    public int UpdateSubmission(Long id, Submission newSubmission) throws DataAccessException {
        String QFSolutionsDeleteSql = "DELETE FROM QuadraticFormulaSolutions WHERE SubmissionId = ?";
        jdbctemplate.update(QFSolutionsDeleteSql, id);

        String PRZSolutionsDeleteSQL = "DELETE FROM PossibleRationalZeroes WHERE SubmissionId = ?";
        jdbctemplate.update(PRZSolutionsDeleteSQL);

        String PolynomialsDeleteSQL = "DELETE FROM Polynomials WHERE SubmissionId = ?";
        jdbctemplate.update(PolynomialsDeleteSQL, id);

        Long inputPolyId = polynomialSave(newSubmission.getInputPolynomial(), id,false, 0, null);

        String SubmissionUpdateSQL = "UPDATE Submissions SET TimeSubmitted = ?, InputPolynomialId = ?";
        jdbctemplate.update(SubmissionUpdateSQL, new Date(System.currentTimeMillis()), inputPolyId);

        for(int i = 0; i < newSubmission.IntermediatePolynomials.size(); i++)
        {
            polynomialSave(newSubmission.IntermediatePolynomials.get(i),
                    id,
                    true,
                    i + 1,
                    newSubmission.FactoredZeroes.get(i)
            );
        }

        String submissionPRZInsertSql = "INSERT INTO PossibleRationalZeroes (Numerator, Denominator, SubmissionId) VALUES (?, ?, ?)";
        jdbctemplate.batchUpdate(submissionPRZInsertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, newSubmission.PossibleRationalZeroes.get(i).getNumerator());
                ps.setInt(2, newSubmission.PossibleRationalZeroes.get(i).getDenominator());
                ps.setLong(3, id);
            }
            @Override
            public int getBatchSize() {
                return newSubmission.PossibleRationalZeroes.size();
            }
        });

        //Save the Quadratic Formula Solution pair, if it exists
        if(newSubmission.QuadraticSolutionPair != null){
            if(newSubmission.QuadraticSolutionPair.getSolutionType() == QuadraticFormulaSolutionType.TWO_REAL)
            {
                jdbctemplate.batchUpdate("INSERT INTO QuadraticFormulaSolutions (ComplexReal, SubmissionId) VALUES (? , ?)",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                ps.setDouble(1, newSubmission.QuadraticSolutionPair.getSolutions().get(i).getReal());
                                ps.setLong(2, id);
                            }

                            @Override
                            public int getBatchSize() {
                                return 2;
                            }
                        });
            }
            else if (newSubmission.QuadraticSolutionPair.getSolutionType() == QuadraticFormulaSolutionType.TWO_COMPLEX) {
                jdbctemplate.batchUpdate("INSERT INTO QuadraticFormulaSolutions (ComplexReal, ComplexImaginary, SubmissionId) VALUES (?, ?, ?)",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                ps.setDouble(1, newSubmission.QuadraticSolutionPair.getSolutions().get(i).getReal());
                                ps.setDouble(2, newSubmission.QuadraticSolutionPair.getSolutions().get(i).getImaginary());
                                ps.setLong(3, id);
                            }

                            @Override
                            public int getBatchSize() {
                                return 2;
                            }
                        });
            }
            else {
                jdbctemplate.update("INSERT INTO QuadraticFormulaSolutions (ComplexReal, SubmissionId) VALUES (?, ? )",
                        newSubmission.QuadraticSolutionPair.getSolutions().get(0).getReal(), id);
            }
        }

        logger.info("Updating of submission with id " + id + " completed"); 
        return 0;
    }

    @Override
    public int DeleteSubmission(long submissionToDeleteId) {
        String DeletionSql = "DELETE FROM Submissions WHERE ID = ?";
        jdbctemplate.update(DeletionSql, submissionToDeleteId);
        return 1;
    }
}

record PRZListSubmissionPair(Long SubmissionId, RationalNumber rn){};
