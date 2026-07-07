package com.leavemanager.dao;

import com.leavemanager.database.DatabaseConnection;
import com.leavemanager.models.LeaveBalance;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LeaveBalanceDAO {

    /**
     * Retrieves the LeaveBalance for an employee.
     */
    public LeaveBalance getLeaveBalance(int employeeId) {
        String query = "SELECT employee_id, casual_leave, sick_leave, earned_leave FROM leave_balance WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new LeaveBalance(
                        rs.getInt("employee_id"),
                        rs.getInt("casual_leave"),
                        rs.getInt("sick_leave"),
                        rs.getInt("earned_leave")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error in getLeaveBalance");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates an employee's leave balance.
     */
    public boolean updateLeaveBalance(LeaveBalance balance) {
        String query = "UPDATE leave_balance SET casual_leave = ?, sick_leave = ?, earned_leave = ? WHERE employee_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, balance.getCasualLeave());
            ps.setInt(2, balance.getSickLeave());
            ps.setInt(3, balance.getEarnedLeave());
            ps.setInt(4, balance.getEmployeeId());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL error in updateLeaveBalance");
            e.printStackTrace();
        }
        return false;
    }
}
