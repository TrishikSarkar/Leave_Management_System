USE leave_management_db;

-- Clear existing data
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE leave_request;
TRUNCATE TABLE leave_balance;
TRUNCATE TABLE employee;
TRUNCATE TABLE administrator;
SET FOREIGN_KEY_CHECKS = 1;

-- Insert default Administrator
-- password: 'admin123' (SHA-256)
INSERT INTO administrator (id, username, password, name) 
VALUES (1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'System Administrator');

-- Insert default Employee
-- password: 'emp123' (SHA-256)
INSERT INTO employee (id, username, password, name, email, department, designation, joining_date)
VALUES (1, 'john_doe', 'e03d3ec8d5035f8721f5dc64546e59ed790dbcb3b7b598fe57057ccd7b683b00', 'John Doe', 'john@example.com', 'IT', 'Software Engineer', '2025-01-15');

-- Initialize default Employee's leave balance
INSERT INTO leave_balance (employee_id, casual_leave, sick_leave, earned_leave)
VALUES (1, 15, 10, 20);
