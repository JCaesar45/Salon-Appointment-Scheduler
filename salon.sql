-- Salon Appointment Scheduler Database Schema

-- Drop database if exists and create new one
DROP DATABASE IF EXISTS salon;
CREATE DATABASE salon;

-- Connect to salon database
\c salon;

-- Create customers table
CREATE TABLE customers (
    customer_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(255) UNIQUE NOT NULL
);

-- Create services table
CREATE TABLE services (
    service_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Create appointments table
CREATE TABLE appointments (
    appointment_id SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(customer_id),
    service_id INTEGER REFERENCES services(service_id),
    time VARCHAR(255) NOT NULL
);

-- Insert default services
INSERT INTO services (service_id, name) VALUES
    (1, 'cut'),
    (2, 'color'),
    (3, 'perm'),
    (4, 'style'),
    (5, 'trim');
