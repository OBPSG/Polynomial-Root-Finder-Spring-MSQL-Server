package com.polynomialrootfinder.jmssql.msqldaos;

import com.polynomialrootfinder.jmssql.models.Polynomial;
import com.polynomialrootfinder.jmssql.models.Submission;
import com.polynomialrootfinder.jmssql.respositories.ISubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class SubmissionRepositoryDAO implements ISubmissionRepository {
    @Autowired
    private JdbcTemplate jdbctemplate;
    @Override
    public int save(Submission submission) {
        jdbctemplate.update("INSERT INTO Polynomials VALUES (?)", (Integer) submission.getInputPolynomial().degree);
        //Get ID of record that was just inserted to use as foreign key
        //TODO(?) Create Helper Classes for getting SQL Identity Values (among other common tasks)
        Long PolyID = jdbctemplate.query("SELECT IDENT_CURRENT('Polynomials')", rs -> (Long) rs.getLong(1));

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

        jdbctemplate.update("INSERT INTO Submissions (UserID, InputPolynomialID, TimeSubmitted) VALUES (? ? ?)", 0, PolyID, new Date(System.currentTimeMillis()));
        return 0;
    }

    @Override
    public Submission findByID(long id) {
        return null;
    }

    @Override
    public List<Submission> findByUser(long userId) {
        return null;
    }

    @Override
    public List<Submission> findRecent() {
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
