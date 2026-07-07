CREATE DATABASE IF NOT EXISTS leave_management_db;
USE leave_management_db;

-- Employee Table
CREATE TABLE IF NOT EXISTS employee (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(256) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    joining_date DATE NOT NULL
);

-- Administrator Table
CREATE TABLE IF NOT EXISTS administrator (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(256) NOT NULL,
    name VARCHAR(100) NOT NULL
);

-- Leave Request Table
CREATE TABLE IF NOT EXISTS leave_request (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Pending', -- 'Pending', 'Approved', 'Rejected'
    applied_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    comments VARCHAR(255),
    FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE
);

-- Leave Balance Table (Balances initialized by Administrator or updated as required)
CREATE TABLE IF NOT EXISTS leave_balance (
    employee_id INT PRIMARY KEY,
    casual_leave INT NOT NULL DEFAULT 0,
    sick_leave INT NOT NULL DEFAULT 0,
    earned_leave INT NOT NULL DEFAULT 0,
    FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE
);
