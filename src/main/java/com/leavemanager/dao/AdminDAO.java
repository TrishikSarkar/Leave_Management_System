package com.leavemanager.dao;

import com.leavemanager.database.DatabaseConnection;
import com.leavemanager.models.Admin;
import com.leavemanager.utilities.SecurityUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDAO {

    /**
     * Authenticates an Administrator using parameterized query to prevent SQL Injection.
     * 
     * @param username plain-text username
     * @param password plain-text password
     * @return Admin object if authenticated successfully, null otherwise
     */
    public Admin authenticate(String username, String password) {
        String query = "SELECT id, username, password, name FROM administrator WHERE username = ? AND password = ?";
        String hashedPassword = SecurityUtils.hashPassword(password);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, username);
            ps.setString(2, hashedPassword);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Admin(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("name")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error during Administrator authentication");
            e.printStackTrace();
        }
        return null;
    }
}
