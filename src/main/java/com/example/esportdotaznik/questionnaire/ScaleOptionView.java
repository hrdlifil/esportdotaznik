package com.example.esportdotaznik.questionnaire;

public record ScaleOptionView(
    long id,
    String label,
    String description,
    Integer numericValue,
    boolean requiresNote,
    boolean exclusiveChoice
) {
}
