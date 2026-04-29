package com.example.esportdotaznik.questionnaire;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class QuestionnaireController {

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("d. M. yyyy HH:mm");
    private static final Pattern SINGLE_CHOICE_PARAM = Pattern.compile("^singleChoiceAnswers\\[(\\d+)]$");
    private static final Pattern MULTI_CHOICE_PARAM = Pattern.compile("^multiChoiceAnswers\\[(\\d+)]$");
    private static final Pattern TEXT_PARAM = Pattern.compile("^textAnswers\\[(\\d+)]$");
    private static final Pattern NUMERIC_PARAM = Pattern.compile("^numericAnswers\\[(\\d+)]$");
    private static final Pattern NOTE_PARAM = Pattern.compile("^notes\\[(\\d+)]$");

    private final QuestionnaireService questionnaireService;

    public QuestionnaireController(QuestionnaireService questionnaireService) {
        this.questionnaireService = questionnaireService;
    }

    @GetMapping("/")
    public String showHome(Model model) {
        model.addAttribute("questionnaires", questionnaireService.listQuestionnaires());
        return "home";
    }

    @GetMapping("/questionnaires/daily/{phase}")
    public String redirectDailyPhase(
        @PathVariable int phase,
        @RequestParam(required = false) String respondentCode
    ) {
        long questionnaireId = questionnaireService.findDailyQuestionnaireIdByPhase(phase);
        if (respondentCode == null || respondentCode.isBlank()) {
            return "redirect:/questionnaires/" + questionnaireId;
        }
        return "redirect:/questionnaires/" + questionnaireId + "?respondentCode=" + respondentCode.trim();
    }

    @GetMapping("/questionnaires/{questionnaireId}")
    public String showQuestionnaire(
        @PathVariable long questionnaireId,
        @RequestParam(required = false) String respondentCode,
        Model model
    ) {
        QuestionnaireView questionnaire = questionnaireService.getQuestionnaire(questionnaireId);
        QuestionnaireForm form = new QuestionnaireForm();
        form.setStartedAt(OffsetDateTime.now().toString());
        if (respondentCode != null) {
            form.setRespondentCode(respondentCode.trim());
        }

        populateFormModel(model, questionnaire, form, Map.of(), List.of());
        return "questionnaire-form";
    }

    @PostMapping("/questionnaires/{questionnaireId}")
    public String submitQuestionnaire(
        @PathVariable long questionnaireId,
        HttpServletRequest request,
        Model model
    ) {
        QuestionnaireView questionnaire = questionnaireService.getQuestionnaire(questionnaireId);
        QuestionnaireForm form = extractForm(request);
        PreparedSubmission preparedSubmission = questionnaireService.prepareSubmission(questionnaire, form);
        form.setRespondentCode(preparedSubmission.respondentIdentifier());
        form.setStartedAt(preparedSubmission.startedAt());

        if (preparedSubmission.hasErrors()) {
            populateFormModel(model, questionnaire, form, preparedSubmission.questionErrors(), preparedSubmission.globalErrors());
            return "questionnaire-form";
        }

        SubmissionResult submissionResult = questionnaireService.submitQuestionnaire(questionnaire, preparedSubmission);
        model.addAttribute("questionnaire", questionnaire);
        model.addAttribute("result", submissionResult);
        model.addAttribute("submittedAtDisplay", formatTimestamp(submissionResult.submittedAt()));
        return "questionnaire-success";
    }

    private QuestionnaireForm extractForm(HttpServletRequest request) {
        QuestionnaireForm form = new QuestionnaireForm();
        form.setRespondentCode(valueOrEmpty(request.getParameter("respondentCode")));
        form.setStartedAt(valueOrEmpty(request.getParameter("startedAt")));

        request.getParameterMap().forEach((parameterName, values) -> {
            Matcher singleChoiceMatcher = SINGLE_CHOICE_PARAM.matcher(parameterName);
            if (singleChoiceMatcher.matches()) {
                form.putSingleChoiceAnswer(Long.parseLong(singleChoiceMatcher.group(1)), firstValue(values));
                return;
            }

            Matcher multiChoiceMatcher = MULTI_CHOICE_PARAM.matcher(parameterName);
            if (multiChoiceMatcher.matches()) {
                form.putMultiChoiceAnswer(Long.parseLong(multiChoiceMatcher.group(1)), List.of(values));
                return;
            }

            Matcher textMatcher = TEXT_PARAM.matcher(parameterName);
            if (textMatcher.matches()) {
                form.putTextAnswer(Long.parseLong(textMatcher.group(1)), firstValue(values));
                return;
            }

            Matcher numericMatcher = NUMERIC_PARAM.matcher(parameterName);
            if (numericMatcher.matches()) {
                form.putNumericAnswer(Long.parseLong(numericMatcher.group(1)), firstValue(values));
                return;
            }

            Matcher noteMatcher = NOTE_PARAM.matcher(parameterName);
            if (noteMatcher.matches()) {
                form.putNote(Long.parseLong(noteMatcher.group(1)), firstValue(values));
            }
        });

        return form;
    }

    private void populateFormModel(
        Model model,
        QuestionnaireView questionnaire,
        QuestionnaireForm form,
        Map<Long, String> questionErrors,
        List<String> globalErrors
    ) {
        model.addAttribute("questionnaire", questionnaire);
        model.addAttribute("questionnaires", questionnaireService.listQuestionnaires());
        model.addAttribute("form", form);
        model.addAttribute("questionErrors", questionErrors);
        model.addAttribute("globalErrors", globalErrors);
        model.addAttribute("startedAtDisplay", formatTimestamp(form.getStartedAt()));
    }

    private String formatTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isBlank()) {
            return "uloží se při odeslání";
        }

        try {
            return DISPLAY_FORMATTER.format(OffsetDateTime.parse(rawTimestamp).atZoneSameInstant(ZoneId.systemDefault()));
        } catch (DateTimeParseException ex) {
            return "uloží se při odeslání";
        }
    }

    private String firstValue(String[] values) {
        return values.length == 0 ? "" : values[0];
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
