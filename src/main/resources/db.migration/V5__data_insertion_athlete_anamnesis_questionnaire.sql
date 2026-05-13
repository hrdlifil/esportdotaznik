-- FTVS - anamnestický dotazník pro sportovce
-- Source: 4_EK_FTVS_A_anamnesticky dotaznik sportovce_OPTAK_final.pdf
-- Notes:
-- 1) The PDF fields completed by a doctor and handwritten signature blocks are not modeled here.
-- 2) Paper prompts that ask for ANO/NE and a detail are modeled as SINGLE_CHOICE with a note on the positive option.
-- 3) Several paper follow-up fields are consolidated into one prompted note so the current answer model can store them cleanly.

BEGIN;

ALTER TABLE answer ALTER COLUMN text_value TYPE TEXT;

INSERT INTO questionnaire (id, name, questionaire_version) VALUES
    (40, 'FTVS - anamnestický dotazník pro sportovce', 1);

INSERT INTO scale (id, name, description) VALUES
    (4001, 'FTVS_ATHLETE_YES_NO_DESCRIBE_IF_YES', 'Binary yes/no response where a positive answer requires a text detail.'),
    (4002, 'FTVS_ATHLETE_YES_NO_DESCRIBE_EITHER', 'Binary yes/no response where both answers require a text detail.'),
    (4003, 'FTVS_ATHLETE_CHILDHOOD_DISEASES', 'Childhood diseases and infections listed in the athlete anamnesis form.'),
    (4004, 'FTVS_ATHLETE_SPORT_LEVEL', 'Sport participation level: elite, performance, or recreational.'),
    (4005, 'FTVS_ATHLETE_OPTIONAL_SPORT_ENTRY', 'Optional secondary or tertiary sport with sport and level captured in a note.'),
    (4006, 'FTVS_ATHLETE_TRAINING_PERIOD', 'Current training period.'),
    (4007, 'FTVS_ATHLETE_WORK_TYPE', 'Predominant non-training work or school activity type.'),
    (4008, 'FTVS_ATHLETE_USE_STATUS_DESCRIBE', 'No/current/former use status with details for current or former use.'),
    (4009, 'FTVS_ATHLETE_CONSENT_SCOPE', 'Consent scopes from the declaration section.');

INSERT INTO scale_option (id, scale_id, label, description, numeric_value) VALUES
    (400101, 4001, 'YES', 'ano - popiš/doplňte', 1),
    (400102, 4001, 'NO', 'ne', 0),
    (400201, 4002, 'YES', 'ano - popiš/doplňte', 1),
    (400202, 4002, 'NO', 'ne - popiš/doplňte', 0),

    (400301, 4003, 'NONE', 'žádné z uvedených', NULL),
    (400302, 4003, 'REPEATED_ANGINA', 'opakované angíny', NULL),
    (400303, 4003, 'REPEATED_OTITIS', 'opakované záněty středního ucha', NULL),
    (400304, 4003, 'SCARLET_FEVER', 'spála', NULL),
    (400305, 4003, 'PNEUMONIA', 'zápal plic', NULL),
    (400306, 4003, 'RUBELLA', 'zarděnky', NULL),
    (400307, 4003, 'MEASLES', 'spalničky', NULL),
    (400308, 4003, 'MUMPS', 'příušnice', NULL),
    (400309, 4003, 'CHICKENPOX', 'plané neštovice', NULL),
    (400310, 4003, 'RHEUMATIC_FEVER', 'revmatická horečka', NULL),
    (400311, 4003, 'REPEATED_INFECTIONS', 'opakované infekce', NULL),
    (400312, 4003, 'OTHER', 'jiné - popiš/doplňte', NULL),

    (400401, 4004, 'ELITE', 'vrcholově', NULL),
    (400402, 4004, 'PERFORMANCE', 'výkonnostně', NULL),
    (400403, 4004, 'RECREATIONAL', 'rekreačně', NULL),

    (400501, 4005, 'NONE', 'neprovozuji další sport', NULL),
    (400502, 4005, 'YES_DESCRIBE', 'provozuji - popiš sport a úroveň', NULL),

    (400601, 4006, 'FULL_PERFORMANCE', 'plný výkon', NULL),
    (400602, 4006, 'PREPARATION', 'přípravné období', NULL),
    (400603, 4006, 'TRANSITION', 'přechodné období', NULL),
    (400604, 4006, 'OTHER', 'jiné - popiš/doplňte', NULL),

    (400701, 4007, 'MENTAL_SCHOOL', 'duševní (škola)', NULL),
    (400702, 4007, 'PHYSICAL', 'fyzická', NULL),
    (400703, 4007, 'COMBINED', 'kombinovaná', NULL),
    (400704, 4007, 'OTHER', 'jiná - popiš/doplňte', NULL),

    (400801, 4008, 'NO', 'ne', 0),
    (400802, 4008, 'YES_DESCRIBE', 'ano - popiš/doplňte', 1),
    (400803, 4008, 'FORMER_DESCRIBE', 'dříve - popiš/doplňte', 2),

    (400901, 4009, 'SPORT_EXAM_RESULTS', 'výsledky sportovní prohlídky', NULL),
    (400902, 4009, 'HEALTH_ASSESSMENT', 'zdravotní posudek', NULL),
    (400903, 4009, 'THIRD_PARTIES', 'třetí osoby mající vztah ke sportovní činnosti sportovce', NULL),
    (400904, 4009, 'HEALTHCARE_WORKERS', 'ostatní dotčení zdravotničtí pracovníci u lékařských zpráv', NULL);

-- Část A - Identifikační údaje
INSERT INTO question (id, question_type_id, text) VALUES
    (500000, 1, 'A0. Identifikační kód respondenta.'),
    (500001, 1, 'A1. Jméno.'),
    (500002, 1, 'A2. Rodné číslo.'),
    (500003, 1, 'A3. Zdravotní pojišťovna.'),
    (500004, 1, 'A4. Bydliště.'),
    (500005, 1, 'A5. Telefon.'),
    (500006, 1, 'A6. Sportovní svaz / klub / oddíl.'),
    (500007, 1, 'A7. Trenér (jméno, kontakt).');

-- Část B - Osobní anamnéza sportovce
INSERT INTO question (id, question_type_id, text) VALUES
    (500008, 4, 'B1. V dětství označte prodělaná onemocnění, případně doplňte další.'),
    (500009, 3, 'B2. Vrozené vady.'),
    (500010, 3, 'B3. Bolesti žaludku, onemocnění jater, žlučové kameny.'),
    (500011, 3, 'B4. Onemocnění ledvin, záněty močového měchýře, onemocnění pohlavního ústrojí.'),
    (500012, 3, 'B5. Diabetes mellitus (cukrovka), onemocnění štítné žlázy.'),
    (500013, 3, 'B6. Ztráta vědomí z jakékoliv příčiny včetně kolapsu při zátěži, křeče.'),
    (500014, 3, 'B7. Opakované bolesti hlavy (migrény).'),
    (500015, 3, 'B8. Onemocnění dýchacího ústrojí (asthma bronchiale, chronická bronchitida, jiné).'),
    (500016, 3, 'B9. Kožní onemocnění.'),
    (500017, 3, 'B10. Alergie (uveďte jaké).'),
    (500018, 3, 'B11. Onemocnění páteře (uveďte jaké).'),
    (500019, 3, 'B12. Onemocnění kloubů a šlach (uveďte jaké).'),
    (500020, 3, 'B13. Nošení brýlí nebo očních čoček.'),
    (500021, 3, 'B14. Neurologické nebo duševní onemocnění (uveďte jaké).'),
    (500022, 3, 'B15. Pravidelně užívané léky (uveďte název, dávkování a důvod užívání).'),
    (500023, 3, 'B16. Současné zdravotní obtíže (uveďte jaké, lokalizaci a trvání).'),
    (500024, 3, 'B17. U žen: menstruační cyklus od kdy.'),
    (500025, 3, 'B18. U žen: poruchy menstruačního rytmu.'),
    (500026, 3, 'B19. U žen: menstruační bolesti.'),
    (500027, 3, 'B20. Operace a úrazy (uveďte jaké a kdy).');

-- Část C - Rodinná anamnéza sportovce
INSERT INTO question (id, question_type_id, text) VALUES
    (500028, 3, 'C1. Vyskytl se v rodině vysoký krevní tlak? Doplňte u koho.'),
    (500029, 3, 'C2. Vyskytla se v rodině cukrovka? Doplňte u koho.'),
    (500030, 3, 'C3. Vyskytl se v rodině infarkt? Doplňte u koho.'),
    (500031, 3, 'C4. Vyskytla se v rodině cévní mozková příhoda? Doplňte u koho.'),
    (500032, 3, 'C5. Vyskytl se v rodině nádor? Doplňte u koho.'),
    (500033, 3, 'C6. Vyskytly se v rodině duševní choroby? Doplňte u koho.'),
    (500034, 3, 'C7. Vyskytla se v rodině epilepsie? Doplňte u koho.');

-- Část D - Sportovní anamnéza
INSERT INTO question (id, question_type_id, text) VALUES
    (500035, 1, 'D1. Druh sportu I (hlavní).'),
    (500036, 3, 'D2. Úroveň provozování hlavního sportu.'),
    (500037, 3, 'D3. Druh sportu II včetně úrovně provozování.'),
    (500038, 3, 'D4. Druh sportu III včetně úrovně provozování.'),
    (500039, 1, 'D5. Dosažené nejlepší sportovní výsledky.'),
    (500040, 2, 'D6. Trénink a soutěže / závody: průměrně dnů v týdnu.'),
    (500041, 1, 'D7. Trénink a soutěže / závody: průměrný denní objem v hodinách.'),
    (500042, 3, 'D8. V jakém tréninkovém období se nyní nacházíte?'),
    (500043, 1, 'D9. Jakým způsobem je prováděna regenerace?'),
    (500044, 1, 'D10. Jakým způsobem je prováděna kompenzace?');

-- Část E - Sociální anamnéza
INSERT INTO question (id, question_type_id, text) VALUES
    (500045, 1, 'E1. Vaše pracovní (netréninková) činnost trvá kolik hodin denně?'),
    (500046, 3, 'E2. Vaše pracovní (netréninková) činnost je převážně jaká?'),
    (500047, 3, 'E3. Kouříte? Při odpovědi ano nebo dříve doplňte kolik cigaret denně a od kdy.'),
    (500048, 3, 'E4. Pijete alkoholické nápoje? Při odpovědi ano nebo dříve doplňte jaké a jak často.'),
    (500049, 3, 'E5. Užíváte jiné návykové látky? Při odpovědi ano nebo dříve doplňte jaké a jak často.'),
    (500050, 3, 'E6. Užíváte potravinové doplňky (např. protein, kreatin, stimulanty)? Doplňte jak často.'),
    (500051, 1, 'E7. Spánek: pravidelnost, nerušenost, počet hodin denně a případné poruchy spánku.'),
    (500052, 3, 'E8. Stravujete se pravidelně (alespoň 3 jídla denně)? Uveďte jak.'),
    (500053, 1, 'E9. Jméno, telefon, případně název pracoviště dětského / praktického / sportovního lékaře.');

-- Část F - Prohlášení
INSERT INTO question (id, question_type_id, text) VALUES
    (500054, 4, 'F1. Souhlas s předáváním výsledků sportovní prohlídky, zdravotního posudku nebo lékařských zpráv vybraným příjemcům.');

INSERT INTO questionnaire_question (id, questionnaire_id, question_id, scale_id) VALUES
    (500000, 40, 500000, NULL),
    (500001, 40, 500001, NULL),
    (500002, 40, 500002, NULL),
    (500003, 40, 500003, NULL),
    (500004, 40, 500004, NULL),
    (500005, 40, 500005, NULL),
    (500006, 40, 500006, NULL),
    (500007, 40, 500007, NULL),
    (500008, 40, 500008, 4003),
    (500009, 40, 500009, 4001),
    (500010, 40, 500010, 4001),
    (500011, 40, 500011, 4001),
    (500012, 40, 500012, 4001),
    (500013, 40, 500013, 4001),
    (500014, 40, 500014, 4001),
    (500015, 40, 500015, 4001),
    (500016, 40, 500016, 4001),
    (500017, 40, 500017, 4001),
    (500018, 40, 500018, 4001),
    (500019, 40, 500019, 4001),
    (500020, 40, 500020, 1),
    (500021, 40, 500021, 4001),
    (500022, 40, 500022, 4001),
    (500023, 40, 500023, 4001),
    (500024, 40, 500024, 4001),
    (500025, 40, 500025, 4001),
    (500026, 40, 500026, 4001),
    (500027, 40, 500027, 4001),
    (500028, 40, 500028, 4001),
    (500029, 40, 500029, 4001),
    (500030, 40, 500030, 4001),
    (500031, 40, 500031, 4001),
    (500032, 40, 500032, 4001),
    (500033, 40, 500033, 4001),
    (500034, 40, 500034, 4001),
    (500035, 40, 500035, NULL),
    (500036, 40, 500036, 4004),
    (500037, 40, 500037, 4005),
    (500038, 40, 500038, 4005),
    (500039, 40, 500039, NULL),
    (500040, 40, 500040, NULL),
    (500041, 40, 500041, NULL),
    (500042, 40, 500042, 4006),
    (500043, 40, 500043, NULL),
    (500044, 40, 500044, NULL),
    (500045, 40, 500045, NULL),
    (500046, 40, 500046, 4007),
    (500047, 40, 500047, 4008),
    (500048, 40, 500048, 4008),
    (500049, 40, 500049, 4008),
    (500050, 40, 500050, 4001),
    (500051, 40, 500051, NULL),
    (500052, 40, 500052, 4002),
    (500053, 40, 500053, NULL),
    (500054, 40, 500054, 4009);

COMMIT;
