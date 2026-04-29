package com.example.esportdotaznik.questionnaire;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuestionnairePersistenceIntegrationTest {

    @Autowired
    private QuestionnaireService questionnaireService;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void submittingInitialQuestionnairePersistsSubmissionAndAllAnswers() {
        QuestionnaireView questionnaire = questionnaireService.getQuestionnaire(1L);
        QuestionnaireForm form = buildValidForm(questionnaire);

        long submissionsBefore = countRows("submission");
        long answersBefore = countRows("answer");

        PreparedSubmission preparedSubmission = questionnaireService.prepareSubmission(questionnaire, form);

        assertThat(preparedSubmission.hasErrors()).isFalse();
        assertThat(preparedSubmission.answers()).hasSize(questionnaire.questionCount());

        SubmissionResult result = questionnaireService.submitQuestionnaire(questionnaire, preparedSubmission);
        assertThat(result.answerCount()).isEqualTo(questionnaire.questionCount());

        assertThat(countRows("submission")).isEqualTo(submissionsBefore + 1);
        assertThat(countRows("answer")).isEqualTo(answersBefore + preparedSubmission.answers().size());

        long persistedAnswerCount = jdbcClient.sql("""
                select count(*)
                from answer
                where submission_id = :submissionId
                """)
            .param("submissionId", result.submissionId())
            .query(Long.class)
            .single();
        assertThat(persistedAnswerCount).isEqualTo(questionnaire.questionCount());

        long coveredQuestionCount = jdbcClient.sql("""
                select count(distinct questionnaire_question_id)
                from answer
                where submission_id = :submissionId
                """)
            .param("submissionId", result.submissionId())
            .query(Long.class)
            .single();
        assertThat(coveredQuestionCount).isEqualTo(questionnaire.questionCount());

        QuestionView generatedRegistrationQuestion = findQuestionByCode(questionnaire, "A0");
        String storedGeneratedRegistrationNumber = jdbcClient.sql("""
                select a.text_value
                from answer a
                where a.submission_id = :submissionId
                  and a.questionnaire_question_id = :questionnaireQuestionId
                """)
            .param("submissionId", result.submissionId())
            .param("questionnaireQuestionId", generatedRegistrationQuestion.questionnaireQuestionId())
            .query(String.class)
            .single();
        assertThat(storedGeneratedRegistrationNumber).isEqualTo("DESH-GENERATED-001");

        String storedRespondentIdentifier = jdbcClient.sql("""
                select r.respondent_identification
                from submission s
                join respondent r on r.id = s.respondent_id
                where s.id = :submissionId
                """)
            .param("submissionId", result.submissionId())
            .query(String.class)
            .single();
        assertThat(storedRespondentIdentifier).isEqualTo("DESH-GENERATED-001");
    }

    @Test
    void submittingInitialQuestionnaireAllowsBlankA7WhenA6IsNo() {
        QuestionnaireView questionnaire = questionnaireService.getQuestionnaire(1L);
        QuestionnaireForm form = buildValidForm(questionnaire);

        QuestionView teamMembershipQuestion = findQuestionByCode(questionnaire, "A6");
        ScaleOptionView noTeamMembershipOption = findOptionByDescription(teamMembershipQuestion, "ne");
        form.putSingleChoiceAnswer(teamMembershipQuestion.questionnaireQuestionId(), Long.toString(noTeamMembershipOption.id()));

        QuestionView teamNameQuestion = findQuestionByCode(questionnaire, "A7");
        form.getTextAnswers().remove(teamNameQuestion.questionnaireQuestionId());

        PreparedSubmission preparedSubmission = questionnaireService.prepareSubmission(questionnaire, form);

        assertThat(preparedSubmission.hasErrors()).isFalse();
        assertThat(preparedSubmission.questionErrors()).doesNotContainKey(teamNameQuestion.questionnaireQuestionId());
        assertThat(preparedSubmission.answers())
            .extracting(StoredAnswer::questionnaireQuestionId)
            .doesNotContain(teamNameQuestion.questionnaireQuestionId());

        SubmissionResult result = questionnaireService.submitQuestionnaire(questionnaire, preparedSubmission);

        assertThat(result.answerCount()).isEqualTo(questionnaire.questionCount() - 1);

        long persistedAnswerCount = jdbcClient.sql("""
                select count(*)
                from answer
                where submission_id = :submissionId
                """)
            .param("submissionId", result.submissionId())
            .query(Long.class)
            .single();
        assertThat(persistedAnswerCount).isEqualTo(questionnaire.questionCount() - 1);
    }

    private QuestionnaireForm buildValidForm(QuestionnaireView questionnaire) {
        QuestionnaireForm form = new QuestionnaireForm();
        form.setStartedAt(OffsetDateTime.now().toString());

        for (QuestionnaireSectionView section : questionnaire.sections()) {
            for (QuestionView question : section.questions()) {
                if (question.isOpenText()) {
                    form.putTextAnswer(question.questionnaireQuestionId(), textAnswerFor(question));
                    continue;
                }

                if (question.isNumericInput()) {
                    form.putNumericAnswer(question.questionnaireQuestionId(), "1");
                    continue;
                }

                if (question.isSingleChoice()) {
                    ScaleOptionView option = question.options().stream()
                        .min(Comparator.comparingLong(ScaleOptionView::id))
                        .orElseThrow();
                    form.putSingleChoiceAnswer(question.questionnaireQuestionId(), Long.toString(option.id()));
                    if (question.noteSupported()) {
                        form.putNote(question.questionnaireQuestionId(), "Poznamka " + question.code());
                    }
                    continue;
                }

                if (question.isMultiChoice()) {
                    ScaleOptionView option = question.options().stream()
                        .filter(candidate -> !candidate.exclusiveChoice())
                        .min(Comparator.comparingLong(ScaleOptionView::id))
                        .orElseGet(() -> question.options().stream().min(Comparator.comparingLong(ScaleOptionView::id)).orElseThrow());
                    form.putMultiChoiceAnswer(question.questionnaireQuestionId(), java.util.List.of(Long.toString(option.id())));
                    if (question.noteSupported()) {
                        form.putNote(question.questionnaireQuestionId(), "Poznamka " + question.code());
                    }
                    continue;
                }

                throw new IllegalStateException("Unsupported question type " + question.questionType());
            }
        }

        return form;
    }

    private String textAnswerFor(QuestionView question) {
        return switch (question.code()) {
            case "A0" -> "DESH-GENERATED-001";
            case "A1" -> "Integration Test";
            case "A2" -> "integration-a2@example.com";
            default -> "Odpoved " + question.code();
        };
    }

    private QuestionView findQuestionByCode(QuestionnaireView questionnaire, String code) {
        return questionnaire.sections().stream()
            .flatMap(section -> section.questions().stream())
            .filter(question -> code.equals(question.code()))
            .findFirst()
            .orElseThrow();
    }

    private ScaleOptionView findOptionByDescription(QuestionView question, String description) {
        return question.options().stream()
            .filter(option -> description.equalsIgnoreCase(option.description()))
            .findFirst()
            .orElseThrow();
    }

    private long countRows(String tableName) {
        return jdbcClient.sql("select count(*) from " + tableName)
            .query(Long.class)
            .single();
    }
}
