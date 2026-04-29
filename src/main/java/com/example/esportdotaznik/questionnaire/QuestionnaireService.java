package com.example.esportdotaznik.questionnaire;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionnaireService {

    private static final Pattern QUESTION_CODE_PATTERN = Pattern.compile("^([A-Z]{1,2}\\d+[a-z]?)\\.\\s*(.+)$");
    private static final Set<String> SKIPPABLE_WHEN_NOT_PLAYED = Set.of("DD6", "DD7", "DD8", "DD9", "DD10", "DD11", "DD12", "DD13");
    private static final List<String> AUTO_SKIP_OPTION_LABELS = List.of("NOT_PLAYED", "NOT_APPLICABLE");
    private static final Map<String, SectionMeta> INITIAL_SECTION_RULES = Map.ofEntries(
        Map.entry("A", new SectionMeta("player", "Část A - Základní údaje o hráči", "Identifikace hráče, týmové zázemí a základní organizační údaje.", false)),
        Map.entry("B", new SectionMeta("profile", "Část B - Esportový profil", "Herní režim, délka session a základní esportový profil.", false)),
        Map.entry("C", new SectionMeta("load", "Část C - Charakter zátěže a návyky", "Kognitivní, emoční a vizuální zátěž během hraní.", false)),
        Map.entry("D", new SectionMeta("movement", "Část D - Pohybové a kompenzační návyky", "Fyzická aktivita, regenerace a kompenzační strategie.", false)),
        Map.entry("E", new SectionMeta("mouse", "Část E - Manipulace s myší a ergonomie zařízení", "Typ myši, styl držení a dopad ergonomie zařízení na výkon.", false)),
        Map.entry("F", new SectionMeta("environment", "Část F - Ergonomie herního prostředí", "Židle, stůl, monitor a pracovní návyky během session.", false)),
        Map.entry("G", new SectionMeta("musculoskeletal", "Část G - Muskuloskeletální obtíže a jejich dopad", "Bolestivost a únava horních končetin, zad a hlavy při hraní.", false)),
        Map.entry("H", new SectionMeta("sleep", "Část H - Spánek a režim", "Spánková pravidelnost, rušivé vlivy a večerní návyky.", false))
    );
    private static final List<NumericSectionMeta> DAILY_SECTION_RULES = List.of(
        new NumericSectionMeta("sleep", "Část A - Spánek a stimulanty", "Spánek z minulé noci a dnešní stimulanty.", 1, 4, false),
        new NumericSectionMeta("load", "Část B - Herní zátěž a načasování", "Denní herní zátěž, délka session a přestávky.", 5, 8, false),
        new NumericSectionMeta("state", "Část C - Stav během hraní", "Soustředění, stres a mentální nebo vizuální únava během dne.", 9, 13, false),
        new NumericSectionMeta("events", "Část D - Mimořádné okolnosti", "Krátký záznam situací, které mohly změnit běžný režim dne.", 14, 14, false),
        new NumericSectionMeta("ai", "Volitelný blok pro 2. fázi s AI aplikací", "Tato část se zobrazuje jen v intervenční fázi s personalizovanou zpětnou vazbou.", 15, 17, true)
    );
    private static final Map<Long, QuestionnaireProfile> QUESTIONNAIRE_PROFILES = Map.of(
        1L,
        new QuestionnaireProfile(
            "Vstupní dotazník",
            "Kompletní vstupní mapování hráče, návyků, ergonomie a spánkového režimu.",
            "DESH-e vstupní anamnéza",
            "Tento formulář odpovídá vstupnímu anamnestickému dotazníku z PDF a ukládá odpovědi přímo do existující databáze.",
            "Vyplnění obvykle zabere 10-15 minut.",
            false,
            null,
            null,
            "Identifikace respondenta se uloží podle odpovědi na položku A0 (vygenerované registrační číslo).",
            "A0",
            null,
            "Vyplnit nový vstupní formulář"
        ),
        2L,
        new QuestionnaireProfile(
            "1. fáze",
            "Denní dotazník / deník účastníka pro první fázi studie.",
            "Denní dotazník účastníka",
            "Vyplnění by mělo zabrat přibližně 2-3 minuty. DD1 se vztahuje k minulé noci, ostatní položky k dnešnímu dni.",
            "Čas zahájení i odeslání se ukládá automaticky.",
            true,
            "Kód účastníka",
            "např. DESH-014",
            "Datum a čas vyplnění aplikace doplní sama při odeslání.",
            null,
            null,
            "Vyplnit další denní záznam"
        ),
        3L,
        new QuestionnaireProfile(
            "2. fáze",
            "Denní dotazník / deník účastníka s AI blokem DD15-DD17.",
            "Denní dotazník účastníka",
            "Vyplnění by mělo zabrat přibližně 2-3 minuty. Součástí této verze je i blok s doporučeními z aplikace.",
            "Čas zahájení i odeslání se ukládá automaticky.",
            true,
            "Kód účastníka",
            "např. DESH-014",
            "Datum a čas vyplnění aplikace doplní sama při odeslání.",
            null,
            null,
            "Vyplnit další denní záznam"
        )
    );

    private final QuestionnaireRepository repository;
    private final Clock clock;

    @Autowired
    public QuestionnaireService(QuestionnaireRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    QuestionnaireService(QuestionnaireRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<QuestionnaireSummary> listQuestionnaires() {
        return repository.findAvailableQuestionnaires().stream()
            .map(summaryRow -> {
                QuestionnaireProfile profile = profileFor(summaryRow.questionnaireId(), summaryRow.title(), summaryRow.version());
                return new QuestionnaireSummary(
                    summaryRow.questionnaireId(),
                    summaryRow.title(),
                    profile.badge(),
                    profile.homeDescription(),
                    summaryRow.questionCount()
                );
            })
            .toList();
    }

    public QuestionnaireView getQuestionnaire(long questionnaireId) {
        List<QuestionnaireRepository.QuestionnaireRow> rows = repository.findQuestionnaireRows(questionnaireId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Požadovaný dotazník nebyl nalezen.");
        }

        QuestionnaireRepository.QuestionnaireRow firstRow = rows.get(0);
        QuestionnaireProfile profile = profileFor(firstRow.questionnaireId(), firstRow.questionnaireName(), firstRow.version());

        Map<Long, QuestionAccumulator> questionsById = new LinkedHashMap<>();
        for (QuestionnaireRepository.QuestionnaireRow row : rows) {
            QuestionDescriptor descriptor = parseQuestion(row.questionText());
            QuestionAccumulator accumulator = questionsById.computeIfAbsent(
                row.questionnaireQuestionId(),
                ignored -> new QuestionAccumulator(
                    row.questionnaireQuestionId(),
                    descriptor.code(),
                    descriptor.prompt(),
                    row.questionTypeCode(),
                    isDailyQuestionnaire(firstRow.questionnaireId()) && SKIPPABLE_WHEN_NOT_PLAYED.contains(descriptor.code())
                )
            );

            if (row.scaleOptionId() != null) {
                accumulator.options().add(new ScaleOptionView(
                    row.scaleOptionId(),
                    row.scaleOptionLabel(),
                    row.scaleOptionDescription(),
                    row.scaleOptionNumericValue(),
                    requiresNoteForOption(row.scaleOptionLabel(), row.scaleOptionDescription()),
                    isExclusiveChoice(row.scaleOptionLabel())
                ));
            }
        }

        Map<String, List<QuestionView>> questionsBySection = new LinkedHashMap<>();
        Map<String, SectionMeta> sectionMetaByKey = new LinkedHashMap<>();
        for (QuestionAccumulator accumulator : questionsById.values()) {
            SectionMeta sectionMeta = resolveSection(firstRow.questionnaireId(), accumulator.code());
            QuestionView questionView = new QuestionView(
                accumulator.questionnaireQuestionId(),
                accumulator.code(),
                accumulator.prompt(),
                accumulator.questionType(),
                helperTextForQuestion(accumulator.code(), accumulator.questionType()),
                accumulator.options().stream().anyMatch(ScaleOptionView::requiresNote),
                noteLabelForQuestion(accumulator.code()),
                accumulator.skippableWhenNoPlay(),
                List.copyOf(accumulator.options())
            );

            questionsBySection.computeIfAbsent(sectionMeta.key(), ignored -> new ArrayList<>()).add(questionView);
            sectionMetaByKey.putIfAbsent(sectionMeta.key(), sectionMeta);
        }

        List<QuestionnaireSectionView> sections = new ArrayList<>();
        for (Map.Entry<String, List<QuestionView>> entry : questionsBySection.entrySet()) {
            SectionMeta meta = sectionMetaByKey.get(entry.getKey());
            sections.add(new QuestionnaireSectionView(
                meta.key(),
                meta.title(),
                meta.description(),
                meta.optional(),
                List.copyOf(entry.getValue())
            ));
        }

        return new QuestionnaireView(
            firstRow.questionnaireId(),
            firstRow.questionnaireName(),
            firstRow.version(),
            profile.badge(),
            profile.eyebrow(),
            profile.introText(),
            profile.completionHint(),
            profile.manualRespondentEntry(),
            profile.respondentFieldLabel(),
            profile.respondentFieldPlaceholder(),
            profile.respondentHelpText(),
            profile.respondentIdentifierQuestionCode(),
            profile.respondentIdentifierFallbackQuestionCode(),
            profile.repeatActionLabel(),
            List.copyOf(sections)
        );
    }

    public long findDailyQuestionnaireIdByPhase(int phase) {
        return switch (phase) {
            case 1 -> 2L;
            case 2 -> 3L;
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Denní dotazník pro zvolenou fázi nebyl nalezen.");
        };
    }

    public PreparedSubmission prepareSubmission(QuestionnaireView questionnaire, QuestionnaireForm form) {
        String startedAt = normalizeTimestamp(form.getStartedAt(), OffsetDateTime.now(clock));
        Map<Long, String> questionErrors = new LinkedHashMap<>();
        List<String> globalErrors = new ArrayList<>();
        List<StoredAnswer> answers = new ArrayList<>();
        Map<String, String> textAnswersByCode = new LinkedHashMap<>();

        boolean noPlaySelected = isNoPlaySelected(findQuestionByCode(questionnaire, "DD5"), form);

        for (QuestionnaireSectionView section : questionnaire.sections()) {
            for (QuestionView question : section.questions()) {
                switch (question.questionType()) {
                    case "OPEN_TEXT" -> handleOpenTextQuestion(questionnaire, question, form, questionErrors, answers, textAnswersByCode);
                    case "NUMERIC_INPUT" -> handleNumericQuestion(question, form, questionErrors, answers);
                    case "SINGLE_CHOICE" -> handleSingleChoiceQuestion(question, form, questionErrors, answers, noPlaySelected);
                    case "MULTI_CHOICE" -> handleMultiChoiceQuestion(question, form, questionErrors, answers);
                    default -> throw new IllegalStateException("Unsupported question type " + question.questionType());
                }
            }
        }

        String respondentIdentifier = resolveRespondentIdentifier(questionnaire, form, textAnswersByCode, globalErrors);
        return new PreparedSubmission(
            respondentIdentifier,
            startedAt,
            List.copyOf(answers),
            Map.copyOf(questionErrors),
            List.copyOf(globalErrors)
        );
    }

    @Transactional
    public SubmissionResult submitQuestionnaire(QuestionnaireView questionnaire, PreparedSubmission preparedSubmission) {
        if (preparedSubmission.hasErrors()) {
            throw new IllegalArgumentException("Prepared submission contains validation errors.");
        }

        OffsetDateTime submittedAt = OffsetDateTime.now(clock);
        long respondentId = repository.findOrCreateRespondentId(preparedSubmission.respondentIdentifier());
        long submissionId = repository.createSubmission(
            questionnaire.questionnaireId(),
            respondentId,
            preparedSubmission.startedAt(),
            submittedAt.toString()
        );
        repository.createAnswers(submissionId, preparedSubmission.answers());

        return new SubmissionResult(
            submissionId,
            preparedSubmission.respondentIdentifier(),
            submittedAt.toString(),
            preparedSubmission.answers().size()
        );
    }

    private void handleOpenTextQuestion(
        QuestionnaireView questionnaire,
        QuestionView question,
        QuestionnaireForm form,
        Map<Long, String> questionErrors,
        List<StoredAnswer> answers,
        Map<String, String> textAnswersByCode
    ) {
        if (normalizeText(form.getTextAnswers().get(question.questionnaireQuestionId())).isBlank()
            && maySkipOpenTextAnswer(questionnaire, question, form)) {
            return;
        }

        String value = normalizeText(form.getTextAnswers().get(question.questionnaireQuestionId()));
        if (value.isBlank()) {
            questionErrors.put(question.questionnaireQuestionId(), "Vyplň stručnou odpověď.");
            return;
        }

        if (value.length() > 255) {
            questionErrors.put(question.questionnaireQuestionId(), "Textová odpověď může mít maximálně 255 znaků.");
            return;
        }

        answers.add(new StoredAnswer(question.questionnaireQuestionId(), null, null, value));
        textAnswersByCode.put(question.code(), value);
    }

    private void handleNumericQuestion(
        QuestionView question,
        QuestionnaireForm form,
        Map<Long, String> questionErrors,
        List<StoredAnswer> answers
    ) {
        String rawValue = normalizeText(form.getNumericAnswers().get(question.questionnaireQuestionId()));
        if (rawValue.isBlank()) {
            questionErrors.put(question.questionnaireQuestionId(), "Vyplň celé číslo.");
            return;
        }

        try {
            long numericValue = Long.parseLong(rawValue);
            if (numericValue < 0) {
                questionErrors.put(question.questionnaireQuestionId(), "Číselná odpověď nemůže být záporná.");
                return;
            }

            answers.add(new StoredAnswer(question.questionnaireQuestionId(), null, numericValue, null));
        } catch (NumberFormatException ex) {
            questionErrors.put(question.questionnaireQuestionId(), "Použij celé číslo bez dalších znaků.");
        }
    }

    private void handleSingleChoiceQuestion(
        QuestionView question,
        QuestionnaireForm form,
        Map<Long, String> questionErrors,
        List<StoredAnswer> answers,
        boolean noPlaySelected
    ) {
        ScaleOptionView selectedOption = resolveSingleChoice(question, form);
        if (selectedOption == null && noPlaySelected && question.skippableWhenNoPlay()) {
            selectedOption = resolveAutoSkippedOption(question).orElse(null);
        }

        if (selectedOption == null) {
            questionErrors.put(question.questionnaireQuestionId(), "Vyber jednu odpověď.");
            return;
        }

        String note = validateAndResolveNote(question, selectedOption.requiresNote(), form.getNotes().get(question.questionnaireQuestionId()), questionErrors);
        if (note == null && selectedOption.requiresNote()) {
            return;
        }

        answers.add(new StoredAnswer(
            question.questionnaireQuestionId(),
            selectedOption.id(),
            selectedOption.numericValue() == null ? null : selectedOption.numericValue().longValue(),
            selectedOption.requiresNote() ? note : null
        ));
    }

    private void handleMultiChoiceQuestion(
        QuestionView question,
        QuestionnaireForm form,
        Map<Long, String> questionErrors,
        List<StoredAnswer> answers
    ) {
        List<ScaleOptionView> selectedOptions = resolveMultiChoice(question, form);
        if (selectedOptions.isEmpty()) {
            questionErrors.put(question.questionnaireQuestionId(), "Vyber alespoň jednu odpověď.");
            return;
        }

        boolean hasExclusiveChoice = selectedOptions.stream().anyMatch(ScaleOptionView::exclusiveChoice);
        if (hasExclusiveChoice && selectedOptions.size() > 1) {
            questionErrors.put(question.questionnaireQuestionId(), "Možnost „žádná / nejsem registrován“ kombinuj jen samostatně.");
            return;
        }

        boolean noteRequired = selectedOptions.stream().anyMatch(ScaleOptionView::requiresNote);
        String note = validateAndResolveNote(question, noteRequired, form.getNotes().get(question.questionnaireQuestionId()), questionErrors);
        if (note == null && noteRequired) {
            return;
        }

        for (ScaleOptionView option : selectedOptions) {
            answers.add(new StoredAnswer(
                question.questionnaireQuestionId(),
                option.id(),
                option.numericValue() == null ? null : option.numericValue().longValue(),
                option.requiresNote() ? note : null
            ));
        }
    }

    private String resolveRespondentIdentifier(
        QuestionnaireView questionnaire,
        QuestionnaireForm form,
        Map<String, String> textAnswersByCode,
        List<String> globalErrors
    ) {
        String respondentIdentifier;
        if (questionnaire.manualRespondentEntry()) {
            respondentIdentifier = normalizeText(form.getRespondentCode());
            if (respondentIdentifier.isBlank()) {
                globalErrors.add("Vyplň kód účastníka.");
                return "";
            }
        } else {
            respondentIdentifier = normalizeText(textAnswersByCode.get(questionnaire.respondentIdentifierQuestionCode()));
            if (respondentIdentifier.isBlank() && questionnaire.respondentIdentifierFallbackQuestionCode() != null) {
                respondentIdentifier = normalizeText(textAnswersByCode.get(questionnaire.respondentIdentifierFallbackQuestionCode()));
            }

            if (respondentIdentifier.isBlank()) {
                globalErrors.add("Pro uložení respondenta vyplň položku " + questionnaire.respondentIdentifierQuestionCode() + ".");
                return "";
            }
        }

        if (respondentIdentifier.length() > 255) {
            globalErrors.add("Identifikace respondenta může mít maximálně 255 znaků.");
            return "";
        }

        return respondentIdentifier;
    }

    private boolean maySkipOpenTextAnswer(QuestionnaireView questionnaire, QuestionView question, QuestionnaireForm form) {
        if (questionnaire.questionnaireId() != 1L) {
            return false;
        }

        if ("A7".equals(question.code())) {
            QuestionView teamMembershipQuestion = findQuestionByCode(questionnaire, "A6");
            ScaleOptionView teamMembership = teamMembershipQuestion == null ? null : resolveSingleChoice(teamMembershipQuestion, form);
            return teamMembership != null && isNegativeChoice(teamMembership);
        }

        return false;
    }

    private boolean isNoPlaySelected(QuestionView question, QuestionnaireForm form) {
        if (question == null) {
            return false;
        }

        ScaleOptionView selectedOption = resolveSingleChoice(question, form);
        return selectedOption != null && "0H".equals(selectedOption.label());
    }

    private QuestionView findQuestionByCode(QuestionnaireView questionnaire, String code) {
        for (QuestionnaireSectionView section : questionnaire.sections()) {
            for (QuestionView question : section.questions()) {
                if (code.equals(question.code())) {
                    return question;
                }
            }
        }
        return null;
    }

    private ScaleOptionView resolveSingleChoice(QuestionView question, QuestionnaireForm form) {
        String rawValue = form.getSingleChoiceAnswers().get(question.questionnaireQuestionId());
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            long optionId = Long.parseLong(rawValue);
            return question.options().stream()
                .filter(option -> option.id() == optionId)
                .findFirst()
                .orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<ScaleOptionView> resolveMultiChoice(QuestionView question, QuestionnaireForm form) {
        List<String> rawValues = form.getMultiChoiceAnswers().get(question.questionnaireQuestionId());
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }

        Set<Long> selectedIds = new LinkedHashSet<>();
        for (String rawValue : rawValues) {
            try {
                selectedIds.add(Long.parseLong(rawValue));
            } catch (NumberFormatException ignored) {
            }
        }

        return question.options().stream()
            .filter(option -> selectedIds.contains(option.id()))
            .toList();
    }

    private Optional<ScaleOptionView> findOptionByLabel(QuestionView question, String label) {
        return question.options().stream().filter(option -> label.equals(option.label())).findFirst();
    }

    private Optional<ScaleOptionView> resolveAutoSkippedOption(QuestionView question) {
        for (String label : AUTO_SKIP_OPTION_LABELS) {
            Optional<ScaleOptionView> option = findOptionByLabel(question, label);
            if (option.isPresent()) {
                return option;
            }
        }
        return Optional.empty();
    }

    private String validateAndResolveNote(
        QuestionView question,
        boolean noteRequired,
        String rawNote,
        Map<Long, String> questionErrors
    ) {
        String note = normalizeText(rawNote);
        if (!noteRequired) {
            return null;
        }

        if (note.isBlank()) {
            questionErrors.put(question.questionnaireQuestionId(), "Doplň i textové upřesnění.");
            return null;
        }

        if (note.length() > 255) {
            questionErrors.put(question.questionnaireQuestionId(), "Textové upřesnění může mít maximálně 255 znaků.");
            return null;
        }

        return note;
    }

    private QuestionDescriptor parseQuestion(String questionText) {
        Matcher matcher = QUESTION_CODE_PATTERN.matcher(questionText);
        if (!matcher.matches()) {
            throw new IllegalStateException("Unexpected questionnaire text format: " + questionText);
        }
        return new QuestionDescriptor(matcher.group(1), matcher.group(2));
    }

    private SectionMeta resolveSection(long questionnaireId, String questionCode) {
        if (isDailyQuestionnaire(questionnaireId)) {
            int questionNumber = Integer.parseInt(questionCode.substring(2));
            return DAILY_SECTION_RULES.stream()
                .filter(section -> questionNumber >= section.start() && questionNumber <= section.end())
                .findFirst()
                .map(NumericSectionMeta::toSectionMeta)
                .orElseThrow(() -> new IllegalStateException("No daily section configured for question code " + questionCode));
        }

        SectionMeta section = INITIAL_SECTION_RULES.get(questionCode.substring(0, 1));
        if (section == null) {
            throw new IllegalStateException("No initial questionnaire section configured for question code " + questionCode);
        }
        return section;
    }

    private boolean requiresNoteForOption(String label, String description) {
        if ("OTHER".equalsIgnoreCase(label)) {
            return true;
        }

        String normalizedDescription = normalizeForMatching(description);
        return normalizedDescription.contains("popis")
            || normalizedDescription.contains("jina")
            || normalizedDescription.contains("jiny")
            || normalizedDescription.contains("jine")
            || normalizedDescription.contains("role");
    }

    private boolean isExclusiveChoice(String label) {
        return "NONE".equalsIgnoreCase(label);
    }

    private boolean isNegativeChoice(ScaleOptionView option) {
        String normalizedDescription = normalizeForMatching(option.description());
        return "ne".equals(normalizedDescription) || "no".equals(normalizedDescription);
    }

    private String noteLabelForQuestion(String code) {
        if ("DD14".equals(code)) {
            return "Krátký popis mimořádné okolnosti";
        }
        return "Doplň upřesnění";
    }

    private String helperTextForQuestion(String questionCode, String questionType) {
        if ("A7".equals(questionCode)) {
            return "Vyplnit jen pokud jsi v A6 odpovedel/a ano.";
        }

        return switch (questionType) {
            case "OPEN_TEXT" -> "Doplň stručnou textovou odpověď.";
            case "NUMERIC_INPUT" -> "Uveď celé číslo.";
            case "MULTI_CHOICE" -> "Lze označit více možností.";
            default -> null;
        };
    }

    private QuestionnaireProfile profileFor(long questionnaireId, String questionnaireName, int version) {
        return QUESTIONNAIRE_PROFILES.getOrDefault(
            questionnaireId,
            new QuestionnaireProfile(
                "Verze " + version,
                questionnaireName,
                "Dotazník",
                "Tento dotazník se načetl přímo z databáze.",
                "Vyplň všechny povinné položky a ulož odpovědi.",
                true,
                "Identifikace respondenta",
                "např. respondent-001",
                "Tato hodnota se uloží do tabulky respondent.",
                null,
                null,
                "Vyplnit znovu"
            )
        );
    }

    private boolean isDailyQuestionnaire(long questionnaireId) {
        return questionnaireId == 2L || questionnaireId == 3L;
    }

    private String normalizeText(String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }

    private String normalizeTimestamp(String rawTimestamp, OffsetDateTime fallback) {
        if (rawTimestamp == null || rawTimestamp.isBlank()) {
            return fallback.toString();
        }

        try {
            return OffsetDateTime.parse(rawTimestamp).toString();
        } catch (DateTimeParseException ex) {
            return fallback.toString();
        }
    }

    private String normalizeForMatching(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private record QuestionnaireProfile(
        String badge,
        String homeDescription,
        String eyebrow,
        String introText,
        String completionHint,
        boolean manualRespondentEntry,
        String respondentFieldLabel,
        String respondentFieldPlaceholder,
        String respondentHelpText,
        String respondentIdentifierQuestionCode,
        String respondentIdentifierFallbackQuestionCode,
        String repeatActionLabel
    ) {
    }

    private record SectionMeta(String key, String title, String description, boolean optional) {
    }

    private record NumericSectionMeta(String key, String title, String description, int start, int end, boolean optional) {

        private SectionMeta toSectionMeta() {
            return new SectionMeta(key, title, description, optional);
        }
    }

    private record QuestionDescriptor(String code, String prompt) {
    }

    private record QuestionAccumulator(
        long questionnaireQuestionId,
        String code,
        String prompt,
        String questionType,
        boolean skippableWhenNoPlay,
        List<ScaleOptionView> options
    ) {
        private QuestionAccumulator(long questionnaireQuestionId, String code, String prompt, String questionType, boolean skippableWhenNoPlay) {
            this(questionnaireQuestionId, code, prompt, questionType, skippableWhenNoPlay, new ArrayList<>());
        }
    }
}
