package com.example.esportdotaznik.questionnaire;

import java.util.List;

public record QuestionView(
    long questionnaireQuestionId,
    String code,
    String prompt,
    String questionType,
    String helperText,
    boolean noteSupported,
    String noteLabel,
    boolean skippableWhenNoPlay,
    List<ScaleOptionView> options
) {

    public boolean isOpenText() {
        return "OPEN_TEXT".equals(questionType);
    }

    public boolean isNumericInput() {
        return "NUMERIC_INPUT".equals(questionType);
    }

    public boolean isSingleChoice() {
        return "SINGLE_CHOICE".equals(questionType);
    }

    public boolean isMultiChoice() {
        return "MULTI_CHOICE".equals(questionType);
    }
}
