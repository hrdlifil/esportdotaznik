package com.example.esportdotaznik.respondent;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RespondentIdentificationRepository {

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public RespondentIdentificationRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public CreatedRespondent createRespondent(String identificationPrefix) {
        lockRespondentTable();

        long respondentId = nextRespondentId();
        String respondentIdentification = identificationPrefix + respondentId;

        jdbcClient.sql("""
                insert into respondent (id, respondent_identification)
                values (:id, :respondentIdentification)
                """)
            .param("id", respondentId)
            .param("respondentIdentification", respondentIdentification)
            .update();

        return new CreatedRespondent(respondentId, respondentIdentification);
    }

    private void lockRespondentTable() {
        jdbcTemplate.execute("lock table respondent in share row exclusive mode");
    }

    private long nextRespondentId() {
        return jdbcClient.sql("select coalesce(max(id), 0) + 1 from respondent")
            .query(Long.class)
            .single();
    }

    public record CreatedRespondent(long respondentId, String respondentIdentification) {
    }
}
