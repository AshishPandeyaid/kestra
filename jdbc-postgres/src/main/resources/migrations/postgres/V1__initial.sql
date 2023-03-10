CREATE TYPE state_type AS ENUM (
    'CREATED',
    'RUNNING',
    'PAUSED',
    'RESTARTED',
    'KILLING',
    'SUCCESS',
    'WARNING',
    'FAILED',
    'KILLED'
);

CREATE TYPE log_level AS ENUM (
    'ERROR',
    'WARN',
    'INFO',
    'DEBUG',
    'TRACE'
);


CREATE TYPE queue_consumers AS ENUM (
    'indexer',
    'executor',
    'worker',
    'scheduler'
);

CREATE TYPE queue_type AS ENUM (
    'io.kestra.core.models.executions.Execution',
    'io.kestra.core.models.flows.Flow',
    'io.kestra.core.models.templates.Template',
    'io.kestra.core.models.executions.ExecutionKilled',
    'io.kestra.core.runners.WorkerTask',
    'io.kestra.core.runners.WorkerTaskResult',
    'io.kestra.core.runners.WorkerInstance',
    'io.kestra.core.runners.WorkerTaskRunning',
    'io.kestra.core.models.executions.LogEntry',
    'io.kestra.core.models.triggers.Trigger'
);

CREATE OR REPLACE FUNCTION FULLTEXT_REPLACE(text, text) RETURNS text
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT
    RETURN TRIM(BOTH $2 FROM REGEXP_REPLACE(COALESCE($1, ''), '[^a-zA-Z\d:]', $2, 'g'));

CREATE OR REPLACE FUNCTION FULLTEXT_INDEX(text) RETURNS tsvector
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT
    RETURN TO_TSVECTOR('simple', FULLTEXT_REPLACE($1, ' ')) || TO_TSVECTOR('simple', $1);

CREATE OR REPLACE FUNCTION FULLTEXT_SEARCH(text) RETURNS tsquery
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT
    RETURN CASE WHEN FULLTEXT_REPLACE($1, '') = '' THEN TO_TSQUERY('')
        ELSE TO_TSQUERY('simple', FULLTEXT_REPLACE($1, ':* & ') || ':*')
    END;

CREATE OR REPLACE FUNCTION STATE_FROMTEXT(text) RETURNS state_type
    LANGUAGE SQL
    IMMUTABLE
    RETURN CAST($1 AS state_type);

CREATE OR REPLACE FUNCTION LOGLEVEL_FROMTEXT(text) RETURNS log_level
    LANGUAGE SQL
    IMMUTABLE
    RETURN CAST($1 AS log_level);

CREATE OR REPLACE FUNCTION PARSE_ISO8601_DATETIME(text) RETURNS timestamptz
    LANGUAGE SQL
    IMMUTABLE
    RETURN $1::timestamptz;

CREATE OR REPLACE FUNCTION PARSE_ISO8601_TIMESTAMP(text) RETURNS int
    LANGUAGE SQL
    IMMUTABLE
    RETURN EXTRACT(epoch FROM $1::timestamptz AT TIME ZONE 'utc');

CREATE OR REPLACE FUNCTION PARSE_ISO8601_DURATION(text) RETURNS interval
    LANGUAGE SQL
    IMMUTABLE
    RETURN $1::interval;;

CREATE OR REPLACE FUNCTION UPDATE_UPDATED_DATETIME() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated = now();
    RETURN NEW;
END;
$$ language 'plpgsql';


/* ----------------------- queues ----------------------- */
CREATE TABLE queues (
    "offset" SERIAL PRIMARY KEY,
    type queue_type NOT NULL,
    key VARCHAR(250) NOT NULL,
    value JSONB NOT NULL,
    consumers queue_consumers[],
    updated TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX queues_type__consumers ON queues (type, consumers, "offset");
CREATE INDEX queues_type__offset ON queues (type, "offset");
CREATE INDEX queues_updated ON queues ("updated");

CREATE TRIGGER queues_updated BEFORE UPDATE
    ON queues FOR EACH ROW EXECUTE PROCEDURE
    UPDATE_UPDATED_DATETIME();


/* ----------------------- flows ----------------------- */
CREATE TABLE flows (
    key VARCHAR(250) NOT NULL PRIMARY KEY,
    value JSONB NOT NULL,
    deleted BOOL NOT NULL GENERATED ALWAYS AS (CAST(value ->> 'deleted' AS BOOL)) STORED,
    id VARCHAR(100) NOT NULL GENERATED ALWAYS AS (value ->> 'id') STORED,
    namespace VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'namespace') STORED,
    revision INT NOT NULL GENERATED ALWAYS AS (CAST(value ->> 'revision' AS INT)) STORED,
    fulltext TSVECTOR GENERATED ALWAYS AS (
        FULLTEXT_INDEX(CAST(value->>'namespace' AS VARCHAR)) ||
        FULLTEXT_INDEX(CAST(value->>'id' AS VARCHAR))
    ) STORED,
    source_code TEXT NOT NULL
);

CREATE INDEX flows_namespace ON flows (deleted, namespace);
CREATE INDEX flows_namespace__id__revision ON flows (deleted, namespace, id, revision);
CREATE INDEX flows_fulltext ON flows USING GIN (fulltext);
CREATE INDEX flows_source_code ON flows USING GIN (FULLTEXT_INDEX(source_code));


/* ----------------------- templates ----------------------- */
CREATE TABLE templates (
    key VARCHAR(250) NOT NULL PRIMARY KEY,
    value JSONB NOT NULL,
    deleted BOOL NOT NULL GENERATED ALWAYS AS (CAST(value ->> 'deleted' AS BOOL)) STORED,
    id VARCHAR(100) NOT NULL GENERATED ALWAYS AS (value ->> 'id') STORED,
    namespace VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'namespace') STORED,
    fulltext TSVECTOR GENERATED ALWAYS AS (FULLTEXT_INDEX(
        FULLTEXT_REPLACE(CAST(value->>'namespace' AS VARCHAR), ' ') || ' ' ||
        FULLTEXT_REPLACE(CAST(value->>'id' AS VARCHAR), ' ')
    )) STORED
);

CREATE INDEX templates_namespace ON templates (deleted, namespace);
CREATE INDEX templates_namespace__id ON templates (deleted, namespace, id);
CREATE INDEX templates_fulltext ON templates USING GIN (fulltext);


/* ----------------------- executions ----------------------- */
CREATE TABLE executions (
    key VARCHAR(250) NOT NULL PRIMARY KEY,
    value JSONB NOT NULL,
    deleted BOOL NOT NULL GENERATED ALWAYS AS (CAST(value ->> 'deleted' AS bool)) STORED,
    namespace VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'namespace') STORED,
    flow_id VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'flowId') STORED,
    state_current state_type NOT NULL GENERATED ALWAYS AS (STATE_FROMTEXT(value #>> '{state, current}')) STORED,
    state_duration BIGINT NOT NULL GENERATED ALWAYS AS (EXTRACT(MILLISECONDS FROM PARSE_ISO8601_DURATION(value #>> '{state, duration}'))) STORED,
    start_date TIMESTAMP NOT NULL GENERATED ALWAYS AS (PARSE_ISO8601_DATETIME(value #>> '{state, startDate}')) STORED,
    end_date TIMESTAMP GENERATED ALWAYS AS (PARSE_ISO8601_DATETIME(value #>> '{state, endDate}')) STORED,
    fulltext TSVECTOR GENERATED ALWAYS AS (
        FULLTEXT_INDEX(CAST(value ->> 'namespace' AS varchar)) ||
        FULLTEXT_INDEX(CAST(value ->> 'flowId' AS varchar)) ||
        FULLTEXT_INDEX(CAST(value ->> 'id' AS varchar))
    ) STORED
);

CREATE INDEX executions_namespace ON executions (deleted, namespace);
CREATE INDEX executions_flow_id ON executions (deleted, flow_id);
CREATE INDEX executions_state_current ON executions (deleted, state_current);
CREATE INDEX executions_start_date ON executions (deleted, start_date);
CREATE INDEX executions_end_date ON executions (deleted, end_date);
CREATE INDEX executions_state_duration ON executions (deleted, state_duration);
CREATE INDEX executions_fulltext ON executions USING GIN (fulltext);


/* ----------------------- triggers ----------------------- */
CREATE TABLE triggers (
    key VARCHAR(250) NOT NULL PRIMARY KEY,
    value JSONB NOT NULL,
    namespace VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'namespace') STORED,
    flow_id VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'flowId') STORED,
    trigger_id VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'triggerId') STORED,
    execution_id VARCHAR(150) GENERATED ALWAYS AS (value ->> 'executionId') STORED
);

CREATE INDEX triggers_execution_id ON triggers (execution_id);


/* ----------------------- logs ----------------------- */
CREATE TABLE logs (
    key VARCHAR(30) NOT NULL PRIMARY KEY,
    value JSONB NOT NULL,
    deleted BOOL NOT NULL GENERATED ALWAYS AS (CAST(value ->> 'deleted' AS bool)) STORED,
    namespace VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'namespace') STORED,
    flow_id VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'flowId') STORED,
    task_id VARCHAR(150) GENERATED ALWAYS AS (value ->> 'taskId') STORED,
    execution_id VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'executionId') STORED,
    taskrun_id VARCHAR(150) GENERATED ALWAYS AS (value ->> 'taskRunId') STORED,
    attempt_number INT GENERATED ALWAYS AS (CAST(value ->> 'attemptNumber' AS INTEGER)) STORED,
    trigger_id VARCHAR(150) GENERATED ALWAYS AS (value ->> 'triggerId') STORED,
    level log_level NOT NULL GENERATED ALWAYS AS (LOGLEVEL_FROMTEXT(value ->> 'level')) STORED,
    timestamp TIMESTAMPTZ NOT NULL GENERATED ALWAYS AS (PARSE_ISO8601_DATETIME(value ->> 'timestamp')) STORED,
    fulltext TSVECTOR GENERATED ALWAYS AS (
        FULLTEXT_INDEX(CAST(value ->> 'namespace' AS varchar)) ||
        FULLTEXT_INDEX(CAST(value ->> 'flowId' AS varchar)) ||
        FULLTEXT_INDEX(COALESCE(CAST(value ->> 'taskId' AS varchar), '')) ||
        FULLTEXT_INDEX(CAST(value ->> 'executionId' AS varchar)) ||
        FULLTEXT_INDEX(COALESCE(CAST(value ->> 'taskRunId' AS varchar), '')) ||
        FULLTEXT_INDEX(COALESCE(CAST(value ->> 'triggerId' AS varchar), '')) ||
        FULLTEXT_INDEX(COALESCE(CAST(value ->> 'message' AS varchar), '')) ||
        FULLTEXT_INDEX(COALESCE(CAST(value ->> 'thread' AS varchar), ''))
    ) STORED
);

CREATE INDEX logs_namespace ON logs (deleted, namespace);
CREATE INDEX logs_execution_id ON logs (deleted, execution_id);
CREATE INDEX logs_execution_id__task_id ON logs (deleted, execution_id, task_id);
CREATE INDEX logs_execution_id__taskrun_id ON logs (deleted, execution_id, taskrun_id);
CREATE INDEX logs_timestamp ON logs (deleted, timestamp);
CREATE INDEX logs_fulltext ON logs USING GIN (fulltext);


/* ----------------------- multipleconditions ----------------------- */
CREATE TABLE multipleconditions (
    key VARCHAR(250) NOT NULL PRIMARY KEY,
    value JSONB NOT NULL,
    namespace VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'namespace') STORED,
    flow_id VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'flowId') STORED,
    condition_id VARCHAR(150) NOT NULL GENERATED ALWAYS AS (value ->> 'conditionId') STORED,
    start_date TIMESTAMPTZ NOT NULL GENERATED ALWAYS AS (PARSE_ISO8601_DATETIME(value ->> 'start')) STORED,
    end_date TIMESTAMPTZ NOT NULL GENERATED ALWAYS AS (PARSE_ISO8601_DATETIME(value ->> 'end')) STORED
);

CREATE INDEX multipleconditions_namespace__flow_id__condition_id ON multipleconditions (namespace, flow_id, condition_id);
CREATE INDEX multipleconditions_start_date__end_date ON multipleconditions (start_date, end_date);


/* ----------------------- workertaskexecutions ----------------------- */
CREATE TABLE workertaskexecutions (
    key VARCHAR(250) NOT NULL PRIMARY KEY,
    value JSONB NOT NULL
);


/* ----------------------- executorstate ----------------------- */
CREATE TABLE executorstate (
    key VARCHAR(250) NOT NULL PRIMARY KEY,
    value JSONB NOT NULL
);


/* ----------------------- executorstate ----------------------- */
CREATE TABLE executordelayed (
    key VARCHAR(250) NOT NULL PRIMARY KEY,
    value JSONB NOT NULL,
    date TIMESTAMPTZ NOT NULL GENERATED ALWAYS AS (PARSE_ISO8601_DATETIME(value ->> 'date')) STORED
);

CREATE INDEX executordelayed_date ON executordelayed (date);
