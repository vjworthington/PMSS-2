-- Test-only schema for integration tests. Mirrors database/PMSS.db so the full
-- JDBC stack exercises the same tables, columns, and constraints as production.
-- Uses IF NOT EXISTS so a cached Spring context re-running init is harmless.

CREATE TABLE IF NOT EXISTS Rooms (
    roomNumber INTEGER PRIMARY KEY AUTOINCREMENT,
    isOccupied BOOLEAN DEFAULT 0 CHECK (isOccupied in (0, 1)),
    fee REAL DEFAULT 100.00,
    roomType TEXT NOT NULL CHECK (roomType in ('REGULAR', 'SPECIAL'))
);

CREATE TABLE IF NOT EXISTS Users (
    userID          INTEGER PRIMARY KEY AUTOINCREMENT,
    userEmail       TEXT NOT NULL UNIQUE,
    passwordHash    TEXT NOT NULL,
    lastName        TEXT NOT NULL,
    firstName       TEXT NOT NULL,
    role            TEXT NOT NULL,
    displayName     TEXT,
    birthDate       TEXT,
    profileImage    BLOB,
    failedAttempts  INTEGER DEFAULT 0,
    lastFailedLogin TEXT,
    lockedTimeTo    TEXT
);

CREATE TABLE IF NOT EXISTS Meetings (
    meetingID   INTEGER PRIMARY KEY AUTOINCREMENT,
    meetingName TEXT NOT NULL,
    meetingDate DATE NOT NULL,
    startTime   TEXT NOT NULL,
    endTime     TEXT NOT NULL,
    userID      INTEGER NOT NULL,
    roomNumber  INTEGER NOT NULL,
    status      TEXT NOT NULL,
    FOREIGN KEY (userID) REFERENCES Users(userID),
    FOREIGN KEY (roomNumber) REFERENCES Rooms(roomNumber)
);

CREATE TABLE IF NOT EXISTS MeetingAttendees (
    meetingID INTEGER NOT NULL,
    userID    INTEGER NOT NULL,
    PRIMARY KEY (meetingID, userID),
    FOREIGN KEY (meetingID) REFERENCES Meetings(meetingID) ON DELETE CASCADE,
    FOREIGN KEY (userID) REFERENCES Users(userID) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Complaints (
    complaintID     INTEGER PRIMARY KEY AUTOINCREMENT,
    meetingID       INTEGER,
    userID          INTEGER NOT NULL,
    complaintOption TEXT,
    summary         TEXT,
    status          TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN PROGRESS', 'RESOLVED')),
    adminResponse   TEXT,
    dateFiled       TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (meetingID) REFERENCES Meetings(meetingID) ON DELETE SET NULL,
    FOREIGN KEY (userID) REFERENCES Users(userID) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS Billing (
    userID         INTEGER PRIMARY KEY,
    cardholderName TEXT,
    cardType       TEXT,
    cardLast4      TEXT,
    cardExpiry     TEXT
);
