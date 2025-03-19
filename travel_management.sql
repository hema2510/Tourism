CREATE DATABASE TravelDB;
USE TravelDB;

CREATE TABLE Users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL
);

INSERT INTO Users (username, password) VALUES ('admin', '12345');

CREATE TABLE Destinations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    country VARCHAR(100),
    price DOUBLE
);
