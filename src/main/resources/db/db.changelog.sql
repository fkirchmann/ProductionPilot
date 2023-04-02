-- liquibase formatted sql
-- Database: PostgreSQL

-- changeset fkirchmann:1
CREATE TABLE Machine
(
    Id BIGSERIAL NOT NULL PRIMARY KEY,
    Name VARCHAR(255) NOT NULL,
    Description TEXT NOT NULL DEFAULT '',
    Deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX machine_unique ON Machine (Name) WHERE Deleted = FALSE;

CREATE TABLE Unit_Of_Measurement
(
    Id BIGSERIAL NOT NULL PRIMARY KEY,
    Name VARCHAR(255) NOT NULL,
    Abbreviation VARCHAR(16) NOT NULL,
    Description TEXT NOT NULL DEFAULT '',
    CONSTRAINT uom_unique UNIQUE (Name, Abbreviation)
);

CREATE TABLE Parameter
(
    Id BIGSERIAL NOT NULL PRIMARY KEY,
    Opc_Node TEXT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Identifier VARCHAR(255),
    Description TEXT NOT NULL DEFAULT '',
    Machine_Id BIGINT NOT NULL REFERENCES Machine(Id),
    Unit_Of_Measurement_Id BIGINT REFERENCES Unit_Of_Measurement(Id) ON DELETE SET NULL,
    Sampling_Interval BIGINT NOT NULL,
    Deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX parameter_unique ON Parameter (Name, Machine_Id) WHERE Deleted = FALSE;
CREATE UNIQUE INDEX parameter_unique_identifier ON Parameter(Identifier) WHERE Deleted = FALSE;

CREATE TABLE Measurement
(
    Id BIGSERIAL NOT NULL PRIMARY KEY,
    Parameter_Id BIGINT NOT NULL REFERENCES Parameter(Id),
    Source_Time TIMESTAMP NOT NULL,
    Server_Time TIMESTAMP NOT NULL,
    Client_Time TIMESTAMP NOT NULL,
    Opc_Status_Code BIGINT NOT NULL,
    Value_String TEXT,
    Value_Boolean BOOLEAN,
    Value_Long BIGINT,
    Value_Double DOUBLE PRECISION,
    CONSTRAINT measurement_nullity CHECK (num_nonnulls(Value_String, Value_Boolean, Value_Long, Value_Double ) = 1)
);

CREATE TABLE Batch
(
    Id BIGSERIAL NOT NULL PRIMARY KEY,
    Name VARCHAR(255) NOT NULL,
    Description TEXT NOT NULL DEFAULT '',
    Parent_Batch_Id BIGINT REFERENCES Batch(Id) ON DELETE CASCADE,
    Creation_Time TIMESTAMP NOT NULL DEFAULT NOW(),
    Modification_Time TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT batch_unique UNIQUE (Name, Parent_Batch_Id)
);

CREATE TABLE Batch_Machine
(
    Id BIGSERIAL NOT NULL PRIMARY KEY,
    Batch_Id BIGINT NOT NULL REFERENCES Batch(Id) ON DELETE CASCADE,
    Machine_Id BIGINT NOT NULL REFERENCES Machine(Id) ON DELETE CASCADE,
    Start_Time TIMESTAMP NOT NULL,
    End_Time TIMESTAMP NOT NULL,
    Creation_Time TIMESTAMP NOT NULL DEFAULT NOW(),
    Modification_Time TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT batch_machine_unique UNIQUE (Batch_Id, Machine_Id, Start_Time, End_Time),
    CONSTRAINT batch_machine_end_time_after_start_time CHECK (End_Time > Start_Time)
);

-- changeset fkirchmann:2
ALTER TABLE Measurement
    ALTER COLUMN Source_Time SET DATA TYPE TIMESTAMP WITH TIME ZONE USING Source_Time AT TIME ZONE 'UTC',
    ALTER COLUMN Server_Time SET DATA TYPE TIMESTAMP WITH TIME ZONE USING Server_Time AT TIME ZONE 'UTC',
    ALTER COLUMN Client_Time SET DATA TYPE TIMESTAMP WITH TIME ZONE USING Client_Time AT TIME ZONE 'UTC';
ALTER TABLE Batch
    ALTER COLUMN Creation_Time SET DATA TYPE TIMESTAMP WITH TIME ZONE USING Creation_Time AT TIME ZONE 'UTC',
    ALTER COLUMN Modification_Time SET DATA TYPE TIMESTAMP WITH TIME ZONE USING Modification_Time AT TIME ZONE 'UTC';
ALTER TABLE Batch_Machine
    ALTER COLUMN Start_Time SET DATA TYPE TIMESTAMP WITH TIME ZONE USING Start_Time AT TIME ZONE 'UTC',
    ALTER COLUMN End_Time SET DATA TYPE TIMESTAMP WITH TIME ZONE USING End_Time AT TIME ZONE 'UTC',
    ALTER COLUMN Creation_Time SET DATA TYPE TIMESTAMP WITH TIME ZONE USING Creation_Time AT TIME ZONE 'UTC',
    ALTER COLUMN Modification_Time SET DATA TYPE TIMESTAMP WITH TIME ZONE USING Modification_Time AT TIME ZONE 'UTC';

-- changeset fkirchmann:3
ALTER TABLE Parameter RENAME COLUMN Opc_Node TO Opc_Node_Id;
UPDATE Parameter SET Opc_Node_Id = Opc_Node_Id::json->>'nodeId';
