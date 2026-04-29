-- DESH-e - denní dotazník / deník účastníka
-- Source questionnaire transcribed from uploaded DOCX.
-- Notes:
-- 1) This script populates questionnaire metadata tables only: questionnaire, question_type, scale, scale_option, question, questionnaire_question.
-- 2) questionnaire.questionaire_version = 1 represents 1. fáze (DD1-DD14), and questionaire_version = 2 represents 2. fáze (DD1-DD17 including the AI-app block DD15-DD17).
-- 3) The paper fields 'Kód účastníka', 'Datum vyplnění' and 'Čas vyplnění' are not inserted as questionnaire items; they map more naturally to respondent.respondent_identification and submission timestamps in this schema.
-- 4) DD14 is modeled as SINGLE_CHOICE. If the respondent selects 'ano - krátce popiš', store the short description in answer.text_value.
-- 5) If DD5 = '0 h', DD6-DD13 may be hidden in the application layer or auto-answered using the dedicated 'dnes jsem nehrál/a' options. The schema has no skip-logic table, so this behavior should be implemented outside SQL.
-- 6) Because the schema has no explicit question_code or display_order column, the questionnaire code (DD1, DD2, ...) is preserved in question.text and display order is preserved by questionnaire_question.id.

BEGIN;

-- Question types (insert only if missing)
INSERT INTO question_type (id, code, description)
SELECT 1, 'OPEN_TEXT', 'Open-ended text answer stored in answer.text_value'
WHERE NOT EXISTS (
    SELECT 1 FROM question_type WHERE code = 'OPEN_TEXT'
);
INSERT INTO question_type (id, code, description)
SELECT 2, 'NUMERIC_INPUT', 'Numeric answer stored in answer.numeric_value'
WHERE NOT EXISTS (
    SELECT 1 FROM question_type WHERE code = 'NUMERIC_INPUT'
);
INSERT INTO question_type (id, code, description)
SELECT 3, 'SINGLE_CHOICE', 'Single selected option stored via answer.scale_option_id'
WHERE NOT EXISTS (
    SELECT 1 FROM question_type WHERE code = 'SINGLE_CHOICE'
);
INSERT INTO question_type (id, code, description)
SELECT 4, 'MULTI_CHOICE', 'Multiple selected options; store one answer row per selected scale_option_id'
WHERE NOT EXISTS (
    SELECT 1 FROM question_type WHERE code = 'MULTI_CHOICE'
);

-- Questionnaire versions
INSERT INTO questionnaire (id, name, questionaire_version) VALUES
                                                               (2, 'DESH-e - denní dotazník / deník účastníka', 1),
                                                               (3, 'DESH-e - denní dotazník / deník účastníka', 2);

-- Scales
INSERT INTO scale (id, name, description) VALUES
                                              (101, 'DD1_SLEEP_QUALITY_5', 'DD1 5-point sleep quality and restorative effect scale.'),
                                              (102, 'DD2_CAFFEINE_SERVINGS', 'DD2 approximate number of coffee or other caffeinated drink servings consumed today.'),
                                              (103, 'DD3_ENERGY_DRINKS', 'DD3 number of energy drinks consumed today.'),
                                              (104, 'DD4_CAFFEINE_AFTER_18', 'DD4 caffeine intake after 18:00.'),
                                              (105, 'DD5_GAMING_DURATION', 'DD5 total time spent in gaming training or matches today.'),
                                              (106, 'DD6_SESSION_DIFFICULTY_5_WITH_NOT_PLAYED', 'DD6 perceived difficulty of the main gaming session, with "dnes jsem nehrál/a".'),
                                              (107, 'DD7_LAST_SESSION_BEFORE_SLEEP', 'DD7 how long before sleep the last gaming session ended, with "dnes jsem nehrál/a".'),
                                              (108, 'DD8_REGULAR_BREAKS', 'DD8 whether regular breaks were taken during longer gaming sessions.'),
                                              (109, 'DD9_CONCENTRATION_5_WITH_NOT_PLAYED', 'DD9 typical concentration during play, with "dnes jsem nehrál/a".'),
                                              (110, 'DD10_CONCENTRATION_DROPS', 'DD10 number of marked concentration or attention drops during play, with "dnes jsem nehrál/a".'),
                                              (111, 'DD11_STRESS_5_WITH_NOT_PLAYED', 'DD11 level of stress or tension during play, with "dnes jsem nehrál/a".'),
                                              (112, 'DD12_MENTAL_FATIGUE_5_WITH_NOT_PLAYED', 'DD12 mental fatigue during play, with "dnes jsem nehrál/a".'),
                                              (113, 'DD13_VISUAL_IMPACT_5_WITH_NOT_PLAYED', 'DD13 impact of visual fatigue / eye irritation / headache on play, with "dnes jsem nehrál/a".'),
                                              (114, 'DD14_EXTRAORDINARY_CIRCUMSTANCES', 'DD14 whether an extraordinary circumstance occurred today; optional detail goes to answer.text_value.'),
                                              (115, 'DD15_APP_NOTIFICATION_RECEIVED', 'DD15 whether an app recommendation or notification was received today.'),
                                              (116, 'DD16_RECOMMENDATION_ADHERENCE', 'DD16 degree of adherence to today''s recommendation or notification.'),
                                              (117, 'DD17_RECOMMENDATION_USEFULNESS_5_WITH_NA', 'DD17 usefulness of today''s recommendation or notification, with "nevztahuje se".');

-- Scale options
INSERT INTO scale_option (id, scale_id, label, description, numeric_value) VALUES
                                                                               (10101, 101, '1', 'velmi špatná', 1),
                                                                               (10102, 101, '2', 'spíše špatná', 2),
                                                                               (10103, 101, '3', 'průměrná', 3),
                                                                               (10104, 101, '4', 'dobrá', 4),
                                                                               (10105, 101, '5', 'velmi dobrá', 5),
                                                                               (10201, 102, '0', '0', 0),
                                                                               (10202, 102, '1', '1', 1),
                                                                               (10203, 102, '2', '2', 2),
                                                                               (10204, 102, '3_PLUS', '3 a více', 3),
                                                                               (10301, 103, '0', '0', 0),
                                                                               (10302, 103, '1', '1', 1),
                                                                               (10303, 103, '2_PLUS', '2 a více', 2),
                                                                               (10401, 104, 'NO', 'ne', 0),
                                                                               (10402, 104, 'YES_1X', 'ano, 1x', 1),
                                                                               (10403, 104, 'YES_2X_PLUS', 'ano, 2x a více', 2),
                                                                               (10501, 105, '0H', '0 h', NULL),
                                                                               (10502, 105, 'LT_1H', 'méně než 1 h', NULL),
                                                                               (10503, 105, '1_2H', '1-2 h', NULL),
                                                                               (10504, 105, '2_4H', '2-4 h', NULL),
                                                                               (10505, 105, '4_6H', '4-6 h', NULL),
                                                                               (10506, 105, 'GT_6H', 'více než 6 h', NULL),
                                                                               (10601, 106, '1', 'velmi lehká', 1),
                                                                               (10602, 106, '2', 'spíše lehká', 2),
                                                                               (10603, 106, '3', 'střední', 3),
                                                                               (10604, 106, '4', 'náročná', 4),
                                                                               (10605, 106, '5', 'velmi náročná', 5),
                                                                               (10606, 106, 'NOT_PLAYED', 'dnes jsem nehrál/a', NULL),
                                                                               (10701, 107, 'GT_3H', 'více než 3 h před spaním', NULL),
                                                                               (10702, 107, '1_3H', '1-3 h před spaním', NULL),
                                                                               (10703, 107, 'LT_1H', 'méně než 1 h před spaním', NULL),
                                                                               (10704, 107, 'JUST_BEFORE_SLEEP', 'těsně před spaním', NULL),
                                                                               (10705, 107, 'NOT_PLAYED', 'dnes jsem nehrál/a', NULL),
                                                                               (10801, 108, 'YES', 'ano', NULL),
                                                                               (10802, 108, 'PARTLY', 'částečně', NULL),
                                                                               (10803, 108, 'NO', 'ne', NULL),
                                                                               (10804, 108, 'NOT_APPLICABLE', 'dnes se mě to netýká', NULL),
                                                                               (10901, 109, '1', 'velmi špatná', 1),
                                                                               (10902, 109, '2', 'spíše slabá', 2),
                                                                               (10903, 109, '3', 'průměrná', 3),
                                                                               (10904, 109, '4', 'dobrá', 4),
                                                                               (10905, 109, '5', 'výborná', 5),
                                                                               (10906, 109, 'NOT_PLAYED', 'dnes jsem nehrál/a', NULL),
                                                                               (11001, 110, '0X', '0x', 0),
                                                                               (11002, 110, '1X', '1x', 1),
                                                                               (11003, 110, '2_3X', '2-3x', 2),
                                                                               (11004, 110, '4X_PLUS', '4x a více', 4),
                                                                               (11005, 110, 'NOT_PLAYED', 'dnes jsem nehrál/a', NULL),
                                                                               (11101, 111, '1', 'žádný / minimální', 1),
                                                                               (11102, 111, '2', 'nízký', 2),
                                                                               (11103, 111, '3', 'střední', 3),
                                                                               (11104, 111, '4', 'vysoký', 4),
                                                                               (11105, 111, '5', 'velmi vysoký', 5),
                                                                               (11106, 111, 'NOT_PLAYED', 'dnes jsem nehrál/a', NULL),
                                                                               (11201, 112, '1', 'žádnou', 1),
                                                                               (11202, 112, '2', 'nízkou', 2),
                                                                               (11203, 112, '3', 'střední', 3),
                                                                               (11204, 112, '4', 'vysokou', 4),
                                                                               (11205, 112, '5', 'velmi silnou', 5),
                                                                               (11206, 112, 'NOT_PLAYED', 'dnes jsem nehrál/a', NULL),
                                                                               (11301, 113, '1', 'vůbec', 1),
                                                                               (11302, 113, '2', 'spíše málo', 2),
                                                                               (11303, 113, '3', 'středně', 3),
                                                                               (11304, 113, '4', 'výrazně', 4),
                                                                               (11305, 113, '5', 'velmi výrazně', 5),
                                                                               (11306, 113, 'NOT_PLAYED', 'dnes jsem nehrál/a', NULL),
                                                                               (11401, 114, 'NO', 'ne', 0),
                                                                               (11402, 114, 'YES', 'ano - krátce popiš', 1),
                                                                               (11501, 115, 'NO', 'ne', 0),
                                                                               (11502, 115, 'YES', 'ano', 1),
                                                                               (11601, 116, 'NOT_AT_ALL', 'vůbec ne', 1),
                                                                               (11602, 116, 'PARTLY', 'částečně', 2),
                                                                               (11603, 116, 'MOSTLY', 'převážně', 3),
                                                                               (11604, 116, 'FULLY', 'zcela', 4),
                                                                               (11605, 116, 'NOT_APPLICABLE', 'nevztahuje se', NULL),
                                                                               (11701, 117, '1', 'vůbec ne', 1),
                                                                               (11702, 117, '2', 'spíše ne', 2),
                                                                               (11703, 117, '3', 'částečně', 3),
                                                                               (11704, 117, '4', 'spíše ano', 4),
                                                                               (11705, 117, '5', 'velmi', 5),
                                                                               (11706, 117, 'NOT_APPLICABLE', 'nevztahuje se', NULL);

-- Část A - Spánek a stimulanty
INSERT INTO question (id, question_type_id, text) VALUES
    (20001, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD1. Jak bys ohodnotil/a kvalitu a obnovující efekt spánku z minulé noci?');
INSERT INTO question (id, question_type_id, text) VALUES
    (20002, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD2. Přibližně kolik dávek kávy nebo jiných kofeinových nápojů jsi dnes vypil/a? (Např. káva, kolový nápoj, kofeinový shot, pre-workout apod.)');
INSERT INTO question (id, question_type_id, text) VALUES
    (20003, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD3. Kolik energy drinků jsi dnes vypil/a?');
INSERT INTO question (id, question_type_id, text) VALUES
    (20004, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD4. Přijal/a jsi dnes kofein po 18:00?');
-- Část B - Herní zátěž a načasování
INSERT INTO question (id, question_type_id, text) VALUES
    (20005, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD5. Kolik času jsi dnes celkem strávil/a herním tréninkem nebo zápasy?');
INSERT INTO question (id, question_type_id, text) VALUES
    (20006, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD6. Jak náročná byla dnešní hlavní herní session? (Vnímaná náročnost celkově: tlak, intenzita, soustředění, soutěžnost.)');
INSERT INTO question (id, question_type_id, text) VALUES
    (20007, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD7. Jak dlouho před spaním skončila tvoje poslední herní session?');
INSERT INTO question (id, question_type_id, text) VALUES
    (20008, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD8. Dělal/a jsi během delších herních session pravidelné pauzy? (Za pravidelnou pauzu považujeme alespoň krátké přerušení každých cca 60-90 minut.)');
-- Část C - Stav během hraní
INSERT INTO question (id, question_type_id, text) VALUES
    (20009, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD9. Jaká byla dnes tvoje typická koncentrace během hraní?');
INSERT INTO question (id, question_type_id, text) VALUES
    (20010, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD10. Kolikrát jsi dnes během hraní zaznamenal/a výrazný pokles koncentrace nebo pozornosti?');
INSERT INTO question (id, question_type_id, text) VALUES
    (20011, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD11. Jak vysoký stres nebo napětí jsi dnes při hraní cítil/a?');
INSERT INTO question (id, question_type_id, text) VALUES
    (20012, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD12. Jak silnou mentální únavu jsi dnes při hraní cítil/a?');
INSERT INTO question (id, question_type_id, text) VALUES
    (20013, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD13. Jak moc dnes vizuální únava, podráždění očí nebo bolest hlavy ovlivnily tvoje hraní?');
-- Část D - Mimořádné okolnosti
INSERT INTO question (id, question_type_id, text) VALUES
    (20014, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD14. Stalo se dnes něco mimořádného, co mohlo výrazně ovlivnit spánek, hraní nebo stres? (Např. nemoc, cestování, zkouška, rodinná událost, alkohol, léky, noční směna apod.)');
-- Volitelný blok pouze pro 2. fázi s AI aplikací
INSERT INTO question (id, question_type_id, text) VALUES
    (20015, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD15. Obdržel/a jsi dnes doporučení nebo upozornění z aplikace?');
INSERT INTO question (id, question_type_id, text) VALUES
    (20016, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD16. Nakolik ses dnešním doporučením nebo upozorněním řídil/a?');
INSERT INTO question (id, question_type_id, text) VALUES
    (20017, (SELECT id FROM question_type WHERE code = 'SINGLE_CHOICE'), 'DD17. Nakolik pro tebe bylo dnešní doporučení nebo upozornění užitečné?');

-- questionnaire_question rows for phase 1 (version 1)
INSERT INTO questionnaire_question (id, questionnaire_id, question_id, scale_id) VALUES
                                                                                     (30001, 2, 20001, 101),
                                                                                     (30002, 2, 20002, 102),
                                                                                     (30003, 2, 20003, 103),
                                                                                     (30004, 2, 20004, 104),
                                                                                     (30005, 2, 20005, 105),
                                                                                     (30006, 2, 20006, 106),
                                                                                     (30007, 2, 20007, 107),
                                                                                     (30008, 2, 20008, 108),
                                                                                     (30009, 2, 20009, 109),
                                                                                     (30010, 2, 20010, 110),
                                                                                     (30011, 2, 20011, 111),
                                                                                     (30012, 2, 20012, 112),
                                                                                     (30013, 2, 20013, 113),
                                                                                     (30014, 2, 20014, 114);

-- questionnaire_question rows for phase 2 (version 2)
INSERT INTO questionnaire_question (id, questionnaire_id, question_id, scale_id) VALUES
                                                                                     (31001, 3, 20001, 101),
                                                                                     (31002, 3, 20002, 102),
                                                                                     (31003, 3, 20003, 103),
                                                                                     (31004, 3, 20004, 104),
                                                                                     (31005, 3, 20005, 105),
                                                                                     (31006, 3, 20006, 106),
                                                                                     (31007, 3, 20007, 107),
                                                                                     (31008, 3, 20008, 108),
                                                                                     (31009, 3, 20009, 109),
                                                                                     (31010, 3, 20010, 110),
                                                                                     (31011, 3, 20011, 111),
                                                                                     (31012, 3, 20012, 112),
                                                                                     (31013, 3, 20013, 113),
                                                                                     (31014, 3, 20014, 114),
                                                                                     (31015, 3, 20015, 115),
                                                                                     (31016, 3, 20016, 116),
                                                                                     (31017, 3, 20017, 117);

COMMIT;