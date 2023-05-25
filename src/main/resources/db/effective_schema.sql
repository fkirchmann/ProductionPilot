-- This is the effective databse schema used by ProductionPilot.
-- It is current as of changeset fkirchmann:4.

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
    Opc_Node_Id TEXT NOT NULL,
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
    Source_Time TIMESTAMP WITH TIME ZONE NOT NULL,
    Server_Time TIMESTAMP WITH TIME ZONE NOT NULL,
    Client_Time TIMESTAMP WITH TIME ZONE NOT NULL,
    Opc_Status_Code BIGINT NOT NULL,
    Value_String TEXT,
    Value_Boolean BOOLEAN,
    Value_Long BIGINT,
    Value_Double DOUBLE PRECISION,
    CONSTRAINT measurement_nullity CHECK (num_nonnulls(Value_String, Value_Boolean, Value_Long, Value_Double) = 1)
);
CREATE INDEX measurement_parameter_id_index ON Measurement (Parameter_Id);

CREATE TABLE Batch
(
    Id BIGSERIAL NOT NULL PRIMARY KEY,
    Name VARCHAR(255) NOT NULL,
    Description TEXT NOT NULL DEFAULT '',
    Parent_Batch_Id BIGINT REFERENCES Batch(Id) ON DELETE CASCADE,
    Creation_Time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    Modification_Time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT batch_unique UNIQUE (Name, Parent_Batch_Id)
);

CREATE TABLE Batch_Machine
(
    Id BIGSERIAL NOT NULL PRIMARY KEY,
    Batch_Id BIGINT NOT NULL REFERENCES Batch(Id) ON DELETE CASCADE,
    Machine_Id BIGINT NOT NULL REFERENCES Machine(Id) ON DELETE CASCADE,
    Start_Time TIMESTAMP WITH TIME ZONE NOT NULL,
    End_Time TIMESTAMP WITH TIME ZONE NOT NULL,
    Creation_Time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    Modification_Time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT batch_machine_unique UNIQUE (Batch_Id, Machine_Id, Start_Time, End_Time),
    CONSTRAINT batch_machine_end_time_after_start_time CHECK (End_Time > Start_Time)
);
