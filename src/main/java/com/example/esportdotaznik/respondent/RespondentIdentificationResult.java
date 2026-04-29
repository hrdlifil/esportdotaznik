package com.example.esportdotaznik.respondent;

public record RespondentIdentificationResult(
    String identificationCode,
    String schoolSegment,
    String serviceSegment,
    String sexSegment,
    String birthYearSegment,
    String gamerLevelSegment,
    String respondentIdSegment
) {
}
