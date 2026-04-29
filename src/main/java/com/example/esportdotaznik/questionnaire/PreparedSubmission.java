package com.example.esportdotaznik.questionnaire;

import java.util.List;
import java.util.Map;

record PreparedSubmission(
    String respondentIdentifier,
    String startedAt,
    List<StoredAnswer> answers,
    Map<Long, String> questionErrors,
    List<String> globalErrors
) {

    boolean hasErrors() {
        return !questionErrors.isEmpty() || !globalErrors.isEmpty();
    }
}
