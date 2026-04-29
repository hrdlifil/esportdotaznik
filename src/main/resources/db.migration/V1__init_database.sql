CREATE TABLE questionnaire (
 id BIGINT PRIMARY KEY,
 name VARCHAR(255) NOT NULL,
 questionaire_version INTEGER NOT NULL
);

CREATE TABLE question_type (
id BIGINT PRIMARY KEY,
code VARCHAR(50) NOT NULL UNIQUE,
description  VARCHAR(255)
);

CREATE TABLE scale (
id BIGINT PRIMARY KEY,
name VARCHAR(255) NOT NULL UNIQUE,
description  VARCHAR(255)
);

CREATE TABLE respondent (
id BIGINT PRIMARY KEY,
respondent_identification VARCHAR(255) NOT NULL
);

CREATE TABLE question (
id BIGINT PRIMARY KEY,
question_type_id  BIGINT NOT NULL,
text TEXT NOT NULL,

CONSTRAINT fk_question_question_type
FOREIGN KEY (question_type_id)
REFERENCES question_type (id)
);

CREATE TABLE scale_option (
id BIGINT PRIMARY KEY,
scale_id BIGINT NOT NULL,
label VARCHAR(50) NOT NULL,
description VARCHAR(255) NOT NULL,
numeric_value  INTEGER,

CONSTRAINT fk_scale_option_scale
FOREIGN KEY (scale_id)
REFERENCES scale (id)

);

CREATE TABLE questionnaire_question (
id BIGINT PRIMARY KEY,
questionnaire_id  BIGINT NOT NULL,
question_id BIGINT NOT NULL,
scale_id BIGINT NULL,

CONSTRAINT fk_qq_questionnaire
FOREIGN KEY (questionnaire_id)
REFERENCES questionnaire (id),


CONSTRAINT fk_qq_question
FOREIGN KEY (question_id)
REFERENCES question (id),

CONSTRAINT fk_qq_scale
FOREIGN KEY (scale_id)
REFERENCES scale (id)

);

CREATE TABLE submission (
id BIGINT PRIMARY KEY,
questionnaire_id BIGINT NOT NULL,
respondent_id BIGINT NOT NULL,
started_at VARCHAR(255) NOT NULL,
submitted_at VARcHAR(255) NULL,


CONSTRAINT fk_submission_questionnaire
FOREIGN KEY (questionnaire_id)
REFERENCES questionnaire (id),

CONSTRAINT fk_submission_respondent
FOREIGN KEY (respondent_id)
REFERENCES respondent (id)

);


CREATE TABLE answer (
id BIGINT PRIMARY KEY,
submission_id BIGINT NOT NULL,
questionnaire_question_id BIGINT NOT NULL,
scale_option_id BIGINT NULL,
text_value VARCHAR(255) NULL,
numeric_value BIGINT NULL,

CONSTRAINT fk_answer_submission
FOREIGN KEY (submission_id)
REFERENCES submission (id),

CONSTRAINT fk_answer_questionnaire_question
FOREIGN KEY (questionnaire_question_id)
REFERENCES questionnaire_question (id),


CONSTRAINT fk_answer_scale_option
FOREIGN KEY (scale_option_id)
REFERENCES scale_option (id)

);





