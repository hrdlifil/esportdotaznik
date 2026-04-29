BEGIN;

-- DESH-e - vstupní anamnestický dotazník
-- A0 is the generated registration number supplied by the respondent.
-- It is used by the application as respondent.respondent_identification when the form is submitted.
INSERT INTO question (id, question_type_id, text) VALUES
    (0, 1, 'A0. Vygenerované registrační číslo.');

INSERT INTO questionnaire_question (id, questionnaire_id, question_id, scale_id) VALUES
    (0, 1, 0, NULL);

COMMIT;
