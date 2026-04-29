package com.example.esportdotaznik.respondent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RespondentIdentificationServiceTest {

    @Test
    void validateRequiresSchoolIcoWhenSchoolIsSelected() {
        RespondentIdentificationRepository repository = mock(RespondentIdentificationRepository.class);
        RespondentIdentificationService service = new RespondentIdentificationService(repository);

        RespondentIdentificationForm form = new RespondentIdentificationForm();
        form.setFromSchool("YES");
        form.setServiceMembership("NONE");
        form.setSex("MALE");
        form.setBirthYear("2004");
        form.setGamerLevel("CASUAL");

        Map<String, String> errors = service.validate(form);

        assertThat(errors).containsEntry("schoolIco", "Vyplň IČO školy.");
    }

    @Test
    void createIdentificationBuildsCodeFromRulesAndPersistedDatabaseId() {
        RespondentIdentificationRepository repository = mock(RespondentIdentificationRepository.class);
        RespondentIdentificationService service = new RespondentIdentificationService(repository);

        RespondentIdentificationForm form = new RespondentIdentificationForm();
        form.setFromSchool("YES");
        form.setSchoolIco("12345678");
        form.setServiceMembership("POLICE");
        form.setSex("MALE");
        form.setBirthYear("2004");
        form.setGamerLevel("PROFESSIONAL");

        when(repository.createRespondent("4567815811043"))
            .thenReturn(new RespondentIdentificationRepository.CreatedRespondent(38L, "456781581104338"));

        RespondentIdentificationResult result = service.createIdentification(form);

        verify(repository).createRespondent("4567815811043");
        assertThat(result.identificationCode()).isEqualTo("456781581104338");
        assertThat(result.schoolSegment()).isEqualTo("45678");
        assertThat(result.serviceSegment()).isEqualTo("158");
        assertThat(result.sexSegment()).isEqualTo("11");
        assertThat(result.birthYearSegment()).isEqualTo("04");
        assertThat(result.gamerLevelSegment()).isEqualTo("3");
        assertThat(result.respondentIdSegment()).isEqualTo("38");
    }

    @Test
    void createIdentificationUsesFallbackSchoolSegmentForNonSchoolProband() {
        RespondentIdentificationRepository repository = mock(RespondentIdentificationRepository.class);
        RespondentIdentificationService service = new RespondentIdentificationService(repository);

        RespondentIdentificationForm form = new RespondentIdentificationForm();
        form.setFromSchool("NO");
        form.setServiceMembership("NONE");
        form.setSex("FEMALE");
        form.setBirthYear("1998");
        form.setGamerLevel("INTERMEDIATE");

        when(repository.createRespondent("6666642022982"))
            .thenReturn(new RespondentIdentificationRepository.CreatedRespondent(1L, "66666420229821"));

        RespondentIdentificationResult result = service.createIdentification(form);

        verify(repository).createRespondent("6666642022982");
        assertThat(result.identificationCode()).isEqualTo("66666420229821");
        assertThat(result.respondentIdSegment()).isEqualTo("1");
    }
}
