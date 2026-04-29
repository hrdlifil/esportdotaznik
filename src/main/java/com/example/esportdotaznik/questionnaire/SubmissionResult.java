package com.example.esportdotaznik.questionnaire;

public record SubmissionResult(long submissionId, String respondentIdentifier, String submittedAt, int answerCount) {
}
