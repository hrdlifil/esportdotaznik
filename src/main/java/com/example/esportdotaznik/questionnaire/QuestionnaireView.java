package com.example.esportdotaznik.questionnaire;

import java.util.List;

public record QuestionnaireView(
    long questionnaireId,
    String title,
    int version,
    String badge,
    String eyebrow,
    String introText,
    String completionHint,
    boolean manualRespondentEntry,
    String respondentFieldLabel,
    String respondentFieldPlaceholder,
    String respondentHelpText,
    String respondentIdentifierQuestionCode,
    String respondentIdentifierFallbackQuestionCode,
    String repeatActionLabel,
    List<QuestionnaireSectionView> sections
) {

    public int questionCount() {
        return sections.stream().mapToInt(section -> section.questions().size()).sum();
    }
}
