-- Status Server — ERPNext-compatible schema
-- Tables follow the ERPNext convention: tab{DocType Name}
-- Standard audit columns (name, creation, modified, owner, docstatus, idx)
-- are included so Frappe can consume or import these records without
-- transformation.

CREATE TABLE IF NOT EXISTS `tabState Transition` (
    -- ERPNext standard fields
    `name`          varchar(140)    NOT NULL,
    `creation`      datetime(6)     DEFAULT NULL,
    `modified`      datetime(6)     DEFAULT NULL,
    `modified_by`   varchar(140)    DEFAULT NULL,
    `owner`         varchar(140)    DEFAULT NULL,
    `docstatus`     tinyint(1)      NOT NULL DEFAULT 0,
    `idx`           int             NOT NULL DEFAULT 0,
    -- domain fields
    `attribute_id`  int             NOT NULL,
    `attribute_name` varchar(255)   NOT NULL,
    `from_state`    varchar(10)     NOT NULL,
    `to_state`      varchar(10)     NOT NULL,
    `transitioned_at` datetime(6)   NOT NULL,
    PRIMARY KEY (`name`),
    INDEX `idx_attribute_id`   (`attribute_id`),
    INDEX `idx_transitioned_at` (`transitioned_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tabDowntime Interval` (
    -- ERPNext standard fields
    `name`              varchar(140)    NOT NULL,
    `creation`          datetime(6)     DEFAULT NULL,
    `modified`          datetime(6)     DEFAULT NULL,
    `modified_by`       varchar(140)    DEFAULT NULL,
    `owner`             varchar(140)    DEFAULT NULL,
    `docstatus`         tinyint(1)      NOT NULL DEFAULT 0,
    `idx`               int             NOT NULL DEFAULT 0,
    -- domain fields
    `attribute_id`      int             NOT NULL,
    `attribute_name`    varchar(255)    NOT NULL,
    `opened_at`         datetime(6)     NOT NULL,
    `closed_at`         datetime(6)     DEFAULT NULL,
    `duration_seconds`  decimal(15,3)   DEFAULT NULL,
    PRIMARY KEY (`name`),
    INDEX `idx_attribute_id` (`attribute_id`),
    INDEX `idx_opened_at`    (`opened_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
