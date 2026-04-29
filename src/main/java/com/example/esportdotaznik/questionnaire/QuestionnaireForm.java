package com.example.esportdotaznik.questionnaire;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuestionnaireForm {

    private String respondentCode = "";
    private String startedAt = "";
    private final Map<Long, String> singleChoiceAnswers = new LinkedHashMap<>();
    private final Map<Long, List<String>> multiChoiceAnswers = new LinkedHashMap<>();
    private final Map<Long, String> textAnswers = new LinkedHashMap<>();
    private final Map<Long, String> numericAnswers = new LinkedHashMap<>();
    private final Map<Long, String> notes = new LinkedHashMap<>();

    public String getRespondentCode() {
        return respondentCode;
    }

    public void setRespondentCode(String respondentCode) {
        this.respondentCode = respondentCode;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public Map<Long, String> getSingleChoiceAnswers() {
        return singleChoiceAnswers;
    }

    public Map<Long, List<String>> getMultiChoiceAnswers() {
        return multiChoiceAnswers;
    }

    public Map<Long, String> getTextAnswers() {
        return textAnswers;
    }

    public Map<Long, String> getNumericAnswers() {
        return numericAnswers;
    }

    public Map<Long, String> getNotes() {
        return notes;
    }

    public void putSingleChoiceAnswer(long questionnaireQuestionId, String optionId) {
        if (optionId != null) {
            singleChoiceAnswers.put(questionnaireQuestionId, optionId);
        }
    }

    public void putMultiChoiceAnswer(long questionnaireQuestionId, List<String> optionIds) {
        multiChoiceAnswers.put(questionnaireQuestionId, new ArrayList<>(optionIds));
    }

    public void putTextAnswer(long questionnaireQuestionId, String value) {
        if (value != null) {
            textAnswers.put(questionnaireQuestionId, value);
        }
    }

    public void putNumericAnswer(long questionnaireQuestionId, String value) {
        if (value != null) {
            numericAnswers.put(questionnaireQuestionId, value);
        }
    }

    public void putNote(long questionnaireQuestionId, String value) {
        if (value != null) {
            notes.put(questionnaireQuestionId, value);
        }
    }
}
