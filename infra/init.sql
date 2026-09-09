CREATE DATABASE b1;
CREATE DATABASE b2;
\connect b1
CREATE TABLE stock (id integer PRIMARY KEY, quantity integer NOT NULL);
INSERT INTO stock VALUES (1, 100);
CREATE TABLE events (sequence bigint GENERATED ALWAYS AS IDENTITY, id varchar(36) PRIMARY KEY, change integer NOT NULL);
\connect b2
CREATE TABLE stock (id integer PRIMARY KEY, quantity integer NOT NULL);
INSERT INTO stock VALUES (1, 100);
CREATE TABLE events (sequence bigint GENERATED ALWAYS AS IDENTITY, id varchar(36) PRIMARY KEY, change integer NOT NULL);
