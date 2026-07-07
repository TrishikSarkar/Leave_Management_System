package com.leavemanager.dao;

import com.leavemanager.database.DatabaseConnection;
import com.leavemanager.models.Employee;
import com.leavemanager.utilities.SecurityUtils;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    /**
     * Authenticates an Employee.
     * 
     * @param username plain-text username
     * @param password plain-text password
     * @return Employee object if authenticated successfully, null otherwise
     */
    public Employee authenticate(String username, String password) {
        String query = "SELECT id, username, password, name, email, department, designation, joining_date FROM employee WHERE username = ? AND password = ?";
        String hashedPassword = SecurityUtils.hashPassword(password);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, username);
            ps.setString(2, hashedPassword);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEmployee(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error during Employee authentication");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves an Employee by ID.
     */
    public Employee getEmployeeById(int id) {
        String query = "SELECT id, username, password, name, email, department, designation, joining_date FROM employee WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEmployee(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error in getEmployeeById");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves all Employees.
     */
    public List<Employee> getAllEmployees() {
        List<Employee> list = new ArrayList<>();
        String query = "SELECT id, username, password, name, email, department, designation, joining_date FROM employee";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                list.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            System.err.println("SQL error in getAllEmployees");
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Adds a new Employee and initializes their leave balance to 0 in a transaction.
     */
    public boolean addEmployee(Employee employee) {
        String insertEmployeeQuery = "INSERT INTO employee (username, password, name, email, department, designation, joining_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertBalanceQuery = "INSERT INTO leave_balance (employee_id, casual_leave, sick_leave, earned_leave) VALUES (?, 0, 0, 0)";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement psEmployee = conn.prepareStatement(insertEmployeeQuery, Statement.RETURN_GENERATED_KEYS)) {
                psEmployee.setString(1, employee.getUsername());
                psEmployee.setString(2, SecurityUtils.hashPassword(employee.getPassword()));
                psEmployee.setString(3, employee.getName());
                psEmployee.setString(4, employee.getEmail());
                psEmployee.setString(5, employee.getDepartment());
                psEmployee.setString(6, employee.getDesignation());
                psEmployee.setDate(7, employee.getJoiningDate());

                int affectedRows = psEmployee.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet generatedKeys = psEmployee.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int employeeId = generatedKeys.getInt(1);
                        employee.setId(employeeId); // Set generated ID in model
                        
                        try (PreparedStatement psBalance = conn.prepareStatement(insertBalanceQuery)) {
                            psBalance.setInt(1, employeeId);
                            psBalance.executeUpdate();
                        }
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            conn.commit(); // Commit transaction
            return true;
        } catch (SQLException e) {
            System.err.println("SQL error in addEmployee transaction. Rolling back.");
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * Updates an existing Employee's record (does not update password if not provided).
     */
    public boolean updateEmployee(Employee employee) {
        boolean hasPasswordUpdate = employee.getPassword() != null && !employee.getPassword().trim().isEmpty();
        String query;
        if (hasPasswordUpdate) {
            query = "UPDATE employee SET username = ?, password = ?, name = ?, email = ?, department = ?, designation = ?, joining_date = ? WHERE id = ?";
        } else {
            query = "UPDATE employee SET username = ?, name = ?, email = ?, department = ?, designation = ?, joining_date = ? WHERE id = ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, employee.getUsername());
            if (hasPasswordUpdate) {
                ps.setString(2, SecurityUtils.hashPassword(employee.getPassword()));
                ps.setString(3, employee.getName());
                ps.setString(4, employee.getEmail());
                ps.setString(5, employee.getDepartment());
                ps.setString(6, employee.getDesignation());
                ps.setDate(7, employee.getJoiningDate());
                ps.setInt(8, employee.getId());
            } else {
                ps.setString(2, employee.getName());
                ps.setString(3, employee.getEmail());
                ps.setString(4, employee.getDepartment());
                ps.setString(5, employee.getDesignation());
                ps.setDate(6, employee.getJoiningDate());
                ps.setInt(7, employee.getId());
            }

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL error in updateEmployee");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Deletes an Employee and cascades to leave balances and requests (due to FOREIGN KEY constraints).
     */
    public boolean deleteEmployee(int id) {
        String query = "DELETE FROM employee WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL error in deleteEmployee");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Searches Employee records by name, username, department, or designation.
     */
    public List<Employee> searchEmployees(String searchQuery) {
        List<Employee> list = new ArrayList<>();
        String query = "SELECT id, username, password, name, email, department, designation, joining_date FROM employee " +
                       "WHERE name LIKE ? OR username LIKE ? OR department LIKE ? OR designation = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            String wildCardQuery = "%" + searchQuery + "%";
            ps.setString(1, wildCardQuery);
            ps.setString(2, wildCardQuery);
            ps.setString(3, wildCardQuery);
            ps.setString(4, searchQuery); // Exact check for designation can also be wildcarded
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEmployee(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error in searchEmployees");
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Gets the total number of employees.
     */
    public int getEmployeeCount() {
        String query = "SELECT COUNT(*) FROM employee";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("SQL error in getEmployeeCount");
            e.printStackTrace();
        }
        return 0;
    }

    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        return new Employee(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("department"),
            rs.getString("designation"),
            rs.getDate("joining_date")
        );
    }
}
