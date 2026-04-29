package com.example.esportdotaznik.respondent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RespondentIdentificationService {

    private static final String YES = "YES";
    private static final String NO = "NO";

    private static final List<OptionView> SCHOOL_OPTIONS = List.of(
        new OptionView(YES, "Ano", "Zobrazí se doplňující pole pro IČO školy."),
        new OptionView(NO, "Ne", "Prvních 5 číslic kódu bude nastaveno na 66666.")
    );

    private static final List<OptionView> SERVICE_OPTIONS = List.of(
        new OptionView("POLICE", "Policie", "Další 3 číslice budou 158."),
        new OptionView("FIREFIGHTERS", "Hasiči", "Další 3 číslice budou 150."),
        new OptionView("RESCUE", "Záchranná služba", "Další 3 číslice budou 155."),
        new OptionView("ARMED_FORCES", "Ozbrojené síly", "Další 3 číslice budou 111."),
        new OptionView("NONE", "Nic z toho", "Další 3 číslice budou 420.")
    );

    private static final List<OptionView> SEX_OPTIONS = List.of(
        new OptionView("MALE", "Muž", "Další 2 číslice budou 11."),
        new OptionView("FEMALE", "Žena", "Další 2 číslice budou 22.")
    );

    private static final List<OptionView> GAMER_LEVEL_OPTIONS = List.of(
        new OptionView("CASUAL", "Casual gamer", "Další číslice bude 1."),
        new OptionView("INTERMEDIATE", "Intermediate gamer", "Další číslice bude 2."),
        new OptionView("PROFESSIONAL", "Professional esport player", "Další číslice bude 3.")
    );

    private static final Map<String, String> SERVICE_CODES = Map.of(
        "POLICE", "158",
        "FIREFIGHTERS", "150",
        "RESCUE", "155",
        "ARMED_FORCES", "111",
        "NONE", "420"
    );

    private static final Map<String, String> SEX_CODES = Map.of(
        "MALE", "11",
        "FEMALE", "22"
    );

    private static final Map<String, String> GAMER_LEVEL_CODES = Map.of(
        "CASUAL", "1",
        "INTERMEDIATE", "2",
        "PROFESSIONAL", "3"
    );

    private final RespondentIdentificationRepository repository;

    public RespondentIdentificationService(RespondentIdentificationRepository repository) {
        this.repository = repository;
    }

    public List<OptionView> schoolOptions() {
        return SCHOOL_OPTIONS;
    }

    public List<OptionView> serviceOptions() {
        return SERVICE_OPTIONS;
    }

    public List<OptionView> sexOptions() {
        return SEX_OPTIONS;
    }

    public List<OptionView> gamerLevelOptions() {
        return GAMER_LEVEL_OPTIONS;
    }

    public Map<String, String> validate(RespondentIdentificationForm form) {
        normalize(form);

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        if (!isAllowedValue(form.getFromSchool(), SCHOOL_OPTIONS)) {
            fieldErrors.put("fromSchool", "Vyber, jestli proband pochází ze školy.");
        }

        if (YES.equals(form.getFromSchool())) {
            if (form.getSchoolIco().isBlank()) {
                fieldErrors.put("schoolIco", "Vyplň IČO školy.");
            } else if (!form.getSchoolIco().matches("\\d{8}")) {
                fieldErrors.put("schoolIco", "IČO školy musí mít přesně 8 číslic.");
            }
        } else {
            form.setSchoolIco("");
        }

        if (!isAllowedValue(form.getServiceMembership(), SERVICE_OPTIONS)) {
            fieldErrors.put("serviceMembership", "Vyber příslušnost probanda.");
        }

        if (!isAllowedValue(form.getSex(), SEX_OPTIONS)) {
            fieldErrors.put("sex", "Vyber pohlaví probanda.");
        }

        if (form.getBirthYear().isBlank()) {
            fieldErrors.put("birthYear", "Vyplň rok narození.");
        } else if (!form.getBirthYear().matches("\\d{4}")) {
            fieldErrors.put("birthYear", "Rok narození zadej jako čtyřciferné číslo.");
        } else {
            int year = Integer.parseInt(form.getBirthYear());
            int currentYear = Year.now().getValue();
            if (year < 1900 || year > currentYear) {
                fieldErrors.put("birthYear", "Rok narození musí být mezi 1900 a " + currentYear + ".");
            }
        }

        if (!isAllowedValue(form.getGamerLevel(), GAMER_LEVEL_OPTIONS)) {
            fieldErrors.put("gamerLevel", "Vyber herní úroveň probanda.");
        }

        return fieldErrors;
    }

    @Transactional
    public RespondentIdentificationResult createIdentification(RespondentIdentificationForm form) {
        Map<String, String> fieldErrors = validate(form);
        if (!fieldErrors.isEmpty()) {
            throw new IllegalArgumentException("Form contains validation errors.");
        }

        String schoolSegment = buildSchoolSegment(form);
        String serviceSegment = SERVICE_CODES.get(form.getServiceMembership());
        String sexSegment = SEX_CODES.get(form.getSex());
        String birthYearSegment = form.getBirthYear().substring(form.getBirthYear().length() - 2);
        String gamerLevelSegment = GAMER_LEVEL_CODES.get(form.getGamerLevel());
        String identificationPrefix = schoolSegment + serviceSegment + sexSegment + birthYearSegment + gamerLevelSegment;
        RespondentIdentificationRepository.CreatedRespondent createdRespondent = repository.createRespondent(identificationPrefix);
        String respondentIdSegment = Long.toString(createdRespondent.respondentId());

        return new RespondentIdentificationResult(
            createdRespondent.respondentIdentification(),
            schoolSegment,
            serviceSegment,
            sexSegment,
            birthYearSegment,
            gamerLevelSegment,
            respondentIdSegment
        );
    }

    private String buildSchoolSegment(RespondentIdentificationForm form) {
        if (!YES.equals(form.getFromSchool())) {
            return "66666";
        }

        String schoolIco = form.getSchoolIco();
        return schoolIco.substring(schoolIco.length() - 5);
    }

    private boolean isAllowedValue(String value, List<OptionView> options) {
        return options.stream().anyMatch(option -> option.value().equals(value));
    }

    private void normalize(RespondentIdentificationForm form) {
        form.setFromSchool(trim(form.getFromSchool()));
        form.setSchoolIco(trim(form.getSchoolIco()).replaceAll("\\D", ""));
        form.setServiceMembership(trim(form.getServiceMembership()));
        form.setSex(trim(form.getSex()));
        form.setBirthYear(trim(form.getBirthYear()));
        form.setGamerLevel(trim(form.getGamerLevel()));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public record OptionView(String value, String label, String description) {
    }
}
