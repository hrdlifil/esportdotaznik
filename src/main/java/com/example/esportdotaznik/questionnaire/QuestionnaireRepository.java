package com.example.esportdotaznik.questionnaire;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class QuestionnaireRepository {

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public QuestionnaireRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<QuestionnaireSummaryRow> findAvailableQuestionnaires() {
        return jdbcClient.sql("""
                select q.id,
                       q.name,
                       q.questionaire_version,
                       count(qq.id) as question_count
                from questionnaire q
                join questionnaire_question qq on qq.questionnaire_id = q.id
                group by q.id, q.name, q.questionaire_version
                order by q.id
                """)
            .query((rs, rowNum) -> new QuestionnaireSummaryRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("questionaire_version"),
                rs.getInt("question_count")
            ))
            .list();
    }

    public List<QuestionnaireRow> findQuestionnaireRows(long questionnaireId) {
        return jdbcClient.sql("""
                select q.id as questionnaire_id,
                       q.name as questionnaire_name,
                       q.questionaire_version,
                       qq.id as questionnaire_question_id,
                       question.text as question_text,
                       qt.code as question_type_code,
                       so.id as scale_option_id,
                       so.label as scale_option_label,
                       so.description as scale_option_description,
                       so.numeric_value as scale_option_numeric_value
                from questionnaire q
                join questionnaire_question qq on qq.questionnaire_id = q.id
                join question on question.id = qq.question_id
                join question_type qt on qt.id = question.question_type_id
                left join scale_option so on so.scale_id = qq.scale_id
                where q.id = :questionnaireId
                order by qq.id, so.id
                """)
            .param("questionnaireId", questionnaireId)
            .query(this::mapQuestionnaireRow)
            .list();
    }

    public long findOrCreateRespondentId(String respondentIdentifier) {
        Optional<Long> existingRespondentId = findRespondentIdByIdentifier(respondentIdentifier);
        if (existingRespondentId.isPresent()) {
            return existingRespondentId.get();
        }

        lockTable("respondent");

        return findRespondentIdByIdentifier(respondentIdentifier)
            .orElseGet(() -> {
                long respondentId = nextId("respondent");
                jdbcClient.sql("""
                        insert into respondent (id, respondent_identification)
                        values (:id, :respondentIdentifier)
                        """)
                    .param("id", respondentId)
                    .param("respondentIdentifier", respondentIdentifier)
                    .update();
                return respondentId;
            });
    }

    public long createSubmission(long questionnaireId, long respondentId, String startedAt, String submittedAt) {
        lockTable("submission");
        long submissionId = nextId("submission");

        jdbcClient.sql("""
                insert into submission (id, questionnaire_id, respondent_id, started_at, submitted_at)
                values (:id, :questionnaireId, :respondentId, :startedAt, :submittedAt)
                """)
            .param("id", submissionId)
            .param("questionnaireId", questionnaireId)
            .param("respondentId", respondentId)
            .param("startedAt", startedAt)
            .param("submittedAt", submittedAt)
            .update();

        return submissionId;
    }

    public void createAnswers(long submissionId, List<StoredAnswer> answers) {
        if (answers.isEmpty()) {
            return;
        }

        lockTable("answer");
        long nextAnswerId = nextId("answer");

        for (StoredAnswer answer : answers) {
            jdbcClient.sql("""
                    insert into answer (id, submission_id, questionnaire_question_id, scale_option_id, text_value, numeric_value)
                    values (:id, :submissionId, :questionnaireQuestionId, :scaleOptionId, :textValue, :numericValue)
                    """)
                .param("id", nextAnswerId++)
                .param("submissionId", submissionId)
                .param("questionnaireQuestionId", answer.questionnaireQuestionId())
                .param("scaleOptionId", answer.scaleOptionId())
                .param("textValue", answer.textValue())
                .param("numericValue", answer.numericValue())
                .update();
        }
    }

    private Optional<Long> findRespondentIdByIdentifier(String respondentIdentifier) {
        return jdbcClient.sql("""
                select id
                from respondent
                where respondent_identification = :respondentIdentifier
                order by id
                fetch first 1 row only
                """)
            .param("respondentIdentifier", respondentIdentifier)
            .query(Long.class)
            .optional();
    }

    private void lockTable(String tableName) {
        jdbcTemplate.execute("lock table " + tableName + " in share row exclusive mode");
    }

    private long nextId(String tableName) {
        return switch (tableName) {
            case "respondent" -> jdbcClient.sql("select coalesce(max(id), 0) + 1 from respondent").query(Long.class).single();
            case "submission" -> jdbcClient.sql("select coalesce(max(id), 0) + 1 from submission").query(Long.class).single();
            case "answer" -> jdbcClient.sql("select coalesce(max(id), 0) + 1 from answer").query(Long.class).single();
            default -> throw new IllegalArgumentException("Unsupported table for id allocation: " + tableName);
        };
    }

    private QuestionnaireRow mapQuestionnaireRow(ResultSet rs, int rowNum) throws SQLException {
        return new QuestionnaireRow(
            rs.getLong("questionnaire_id"),
            rs.getString("questionnaire_name"),
            rs.getInt("questionaire_version"),
            rs.getLong("questionnaire_question_id"),
            rs.getString("question_text"),
            rs.getString("question_type_code"),
            rs.getObject("scale_option_id", Long.class),
            rs.getString("scale_option_label"),
            rs.getString("scale_option_description"),
            rs.getObject("scale_option_numeric_value", Integer.class)
        );
    }

    record QuestionnaireSummaryRow(long questionnaireId, String title, int version, int questionCount) {
    }

    record QuestionnaireRow(
        long questionnaireId,
        String questionnaireName,
        int version,
        long questionnaireQuestionId,
        String questionText,
        String questionTypeCode,
        Long scaleOptionId,
        String scaleOptionLabel,
        String scaleOptionDescription,
        Integer scaleOptionNumericValue
    ) {
    }
}
