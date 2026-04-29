package com.example.esportdotaznik.questionnaire;

public record QuestionnaireSummary(
    long questionnaireId,
    String title,
    String badge,
    String description,
    int questionCount
) {
}
