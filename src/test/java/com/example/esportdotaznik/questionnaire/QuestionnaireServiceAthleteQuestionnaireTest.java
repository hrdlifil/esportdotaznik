package com.example.esportdotaznik.questionnaire;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionnaireServiceAthleteQuestionnaireTest {

    private static final long ATHLETE_QUESTIONNAIRE_ID = 40L;

    private final QuestionnaireRepository repository = mock(QuestionnaireRepository.class);
    private final QuestionnaireService service = new QuestionnaireService(
        repository,
        Clock.fixed(Instant.parse("2026-05-13T10:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void athleteQuestionnaireUsesA0AsRespondentIdentifierAndOwnSections() {
        when(repository.findQuestionnaireRows(ATHLETE_QUESTIONNAIRE_ID)).thenReturn(List.of(
            row(500000, "A0. Identifikační kód respondenta.", "OPEN_TEXT", null, null, null, null),
            row(500009, "B2. Vrozené vady.", "SINGLE_CHOICE", 400101L, "YES", "ano - popiš/doplňte", 1),
            row(500009, "B2. Vrozené vady.", "SINGLE_CHOICE", 400102L, "NO", "ne", 0)
        ));

        QuestionnaireView questionnaire = service.getQuestionnaire(ATHLETE_QUESTIONNAIRE_ID);

        assertThat(questionnaire.badge()).isEqualTo("Anamnéza sportovce");
        assertThat(questionnaire.manualRespondentEntry()).isFalse();
        assertThat(questionnaire.respondentIdentifierQuestionCode()).isEqualTo("A0");
        assertThat(questionnaire.sections())
            .extracting(QuestionnaireSectionView::title)
            .containsExactly("Část A - Identifikační údaje", "Část B - Osobní anamnéza sportovce");

        QuestionnaireForm form = new QuestionnaireForm();
        form.putTextAnswer(500000L, "ATH-001");
        form.putSingleChoiceAnswer(500009L, "400102");

        PreparedSubmission preparedSubmission = service.prepareSubmission(questionnaire, form);

        assertThat(preparedSubmission.hasErrors()).isFalse();
        assertThat(preparedSubmission.respondentIdentifier()).isEqualTo("ATH-001");
        assertThat(preparedSubmission.answers())
            .extracting(StoredAnswer::questionnaireQuestionId)
            .containsExactly(500000L, 500009L);
    }

    private QuestionnaireRepository.QuestionnaireRow row(
        long questionnaireQuestionId,
        String questionText,
        String questionTypeCode,
        Long scaleOptionId,
        String scaleOptionLabel,
        String scaleOptionDescription,
        Integer scaleOptionNumericValue
    ) {
        return new QuestionnaireRepository.QuestionnaireRow(
            ATHLETE_QUESTIONNAIRE_ID,
            "FTVS - anamnestický dotazník pro sportovce",
            1,
            questionnaireQuestionId,
            questionText,
            questionTypeCode,
            scaleOptionId,
            scaleOptionLabel,
            scaleOptionDescription,
            scaleOptionNumericValue
        );
    }
}
