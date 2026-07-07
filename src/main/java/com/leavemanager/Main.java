package com.leavemanager;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.leavemanager.database.DatabaseConnection;
import com.leavemanager.ui.LoginFrame;
import javax.swing.SwingUtilities;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        System.out.println("Employee Leave Management System Starting...");
        
        // Initial database connection validation
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Database connectivity verified.");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Unable to connect to MySQL database.");
            e.printStackTrace();
            System.exit(1);
        }
        
        // Initialize Look and Feel
        FlatIntelliJLaf.setup();
        
        // Launch Main Login Screen
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
