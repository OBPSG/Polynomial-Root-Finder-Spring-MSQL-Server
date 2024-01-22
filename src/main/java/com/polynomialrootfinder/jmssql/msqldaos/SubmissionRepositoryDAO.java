package com.polynomialrootfinder.jmssql.msqldaos;

import com.polynomialrootfinder.jmssql.models.Polynomial;
import com.polynomialrootfinder.jmssql.models.Submission;
import com.polynomialrootfinder.jmssql.respositories.ISubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SubmissionRepositoryDAO implements ISubmissionRepository {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionRepositoryDAO.class);

    public class PolynomialTermMapper implements RowMapper<Polynomial.PolynomialTerm>{
        @Override
        public Polynomial.PolynomialTerm mapRow(ResultSet rs, int rowNum) throws SQLException {
            Polynomial.PolynomialTerm newTerm = new Polynomial.PolynomialTerm();
            newTerm.setCoefficient(rs.getInt("Coefficient"));
            newTerm.setExponent(rs.getInt("Exponent"));
            newTerm.setVariable(rs.getString("Variable"));
            return newTerm;
        }
    }

    public class PolynomialRSE implements ResultSetExtractor<Polynomial>{

        @Override
        public Polynomial extractData(ResultSet rs) throws SQLException, DataAccessException {
            return null;
        }
    }


    @Autowired
    private JdbcTemplate jdbctemplate;
    @Override
    public int save(Submission submission) throws DataAccessException {
        jdbctemplate.update("INSERT INTO Polynomials VALUES (?)", (Integer) submission.getInputPolynomial().degree);
        //Get ID of record that was just inserted to use as foreign key
        //TODO(?) Create Helper Classes for getting SQL Identity Values (among other common tasks) Or use ExecuteAndReturnKey?
        Long PolyID = jdbctemplate.query("SELECT IDENT_CURRENT('Polynomials')", (ResultSetExtractor<Long>) rs -> {
            if(rs.next()){
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

        jdbctemplate.update("INSERT INTO Submissions (UserID, InputPolynomialId, TimeSubmitted) VALUES (?, ?, ?)", (Object) 0, PolyID, new Date(System.currentTimeMillis()));
        return 0;
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
                Date timeSubmitted = null;
                while(rs.next()) {
                    if(rs.getRow() == 1) {
                        userId = rs.getInt("UserID");
                        timeSubmitted = rs.getDate("TimeSubmitted");
                        polynomial.degree = rs.getInt("degree");
                        polynomial.terms= new ArrayList<>();
                    }
                    polynomial.terms.add(new PolynomialTermMapper().mapRow(rs, rs.getRow()));
                }
                Submission submission = new Submission(userId, polynomial);
                submission.timeSubmitted = timeSubmitted;
                return submission;
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
        String SubmissionsQuery = "SELECT Submissions.UserID, Submissions.TimeSubmitted, Polynomials.degree, PolynomialTerms.Coefficient, PolynomialTerms.Variable, PolynomialTerms.Exponent FROM Submissions\n" +
                "JOIN Polynomials ON Submissions.InputPolynomialId = Polynomials.Id\n" +
                "Join PolynomialTerms on Polynomials.Id = PolynomialTerms.PolynomialID;";
       List<Submission> polynomials = jdbctemplate.query(SubmissionsQuery, new ResultSetExtractor<List<Submission>>() {
           @Override
           public List<Submission> extractData(ResultSet rs) throws SQLException, DataAccessException {
               return null;
           }
       });
       return null;
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
