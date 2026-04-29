package com.example.esportdotaznik.questionnaire;

import java.util.List;

public record QuestionnaireSectionView(
    String key,
    String title,
    String description,
    boolean optional,
    List<QuestionView> questions
) {
}
