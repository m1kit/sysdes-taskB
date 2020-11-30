# -- Table definitions

# --- !Ups
CREATE TABLE enquete
(
    id         int PRIMARY KEY AUTO_INCREMENT,
    name       varchar(32)                           NOT NULL,
    gender     varchar(6)                            NOT NULL,
    message    varchar(255)                          NOT NULL,
    created_at timestamp default CURRENT_TIMESTAMP() NOT NULL
);

CREATE TABLE task
(
    id           int PRIMARY KEY AUTO_INCREMENT,
    title        varchar(32)  NOT NULL,
    description  varchar(255) NOT NULL,
    state        int          NOT NULL DEFAULT 0,
    cycle        int                   DEFAULT NULL,
    created_at   timestamp             DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deadline     timestamp             DEFAULT NULL,
    completed_at timestamp             DEFAULT NULL
);

CREATE TABLE user
(
    id         int PRIMARY KEY AUTO_INCREMENT,
    name       varchar(32)                           NOT NULL UNIQUE,
    password   varchar(64)                           NOT NULL,
    created_at timestamp default CURRENT_TIMESTAMP() NOT NULL
);

CREATE TABLE task_user
(
    task_id int NOT NULL,
    user_id int NOT NULL,
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id) REFERENCES task (id),
    FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE TABLE task_tag
(
    task_id int         NOT NULL,
    tag     varchar(32) NOT NULL,
    PRIMARY KEY (task_id, tag),
    FOREIGN KEY (task_id) REFERENCES task (id)
);

# --- !Downs
DROP TABLE task_tag;
DROP TABLE task_user;
DROP TABLE user;
DROP TABLE task;
DROP TABLE enquete;
