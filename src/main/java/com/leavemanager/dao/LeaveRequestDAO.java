package com.leavemanager.dao;

import com.leavemanager.database.DatabaseConnection;
import com.leavemanager.models.LeaveRequest;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class LeaveRequestDAO {

    /**
     * Inserts a new leave request into the database.
     */
    public boolean insertLeaveRequest(LeaveRequest request) {
        String query = "INSERT INTO leave_request (employee_id, leave_type, start_date, end_date, reason, status, applied_date) VALUES (?, ?, ?, ?, ?, 'Pending', ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, request.getEmployeeId());
            ps.setString(2, request.getLeaveType());
            ps.setDate(3, request.getStartDate());
            ps.setDate(4, request.getEndDate());
            ps.setString(5, request.getReason());
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL error in insertLeaveRequest");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retrieves leave requests history for a specific employee.
     */
    public List<LeaveRequest> getLeaveHistoryByEmployee(int employeeId) {
        List<LeaveRequest> list = new ArrayList<>();
        String query = "SELECT r.id, r.employee_id, r.leave_type, r.start_date, r.end_date, r.reason, r.status, r.applied_date, r.comments, e.name AS employee_name " +
                       "FROM leave_request r JOIN employee e ON r.employee_id = e.id " +
                       "WHERE r.employee_id = ? ORDER BY r.applied_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLeaveRequest(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error in getLeaveHistoryByEmployee");
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves pending leave requests with employee names.
     */
    public List<LeaveRequest> getPendingLeaveRequests() {
        List<LeaveRequest> list = new ArrayList<>();
        String query = "SELECT r.id, r.employee_id, r.leave_type, r.start_date, r.end_date, r.reason, r.status, r.applied_date, r.comments, e.name AS employee_name " +
                       "FROM leave_request r JOIN employee e ON r.employee_id = e.id " +
                       "WHERE r.status = 'Pending' ORDER BY r.applied_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                list.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            System.err.println("SQL error in getPendingLeaveRequests");
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves all leave requests.
     */
    public List<LeaveRequest> getAllLeaveRequests() {
        List<LeaveRequest> list = new ArrayList<>();
        String query = "SELECT r.id, r.employee_id, r.leave_type, r.start_date, r.end_date, r.reason, r.status, r.applied_date, r.comments, e.name AS employee_name " +
                       "FROM leave_request r JOIN employee e ON r.employee_id = e.id " +
                       "ORDER BY r.applied_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                list.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            System.err.println("SQL error in getAllLeaveRequests");
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Rejects a leave request by updating status and comments.
     */
    public boolean rejectLeaveRequest(int requestId, String comments) {
        String query = "UPDATE leave_request SET status = 'Rejected', comments = ? WHERE id = ? AND status = 'Pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, comments);
            ps.setInt(2, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL error in rejectLeaveRequest");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Approves a leave request. Deducts the leave balance and updates the request status inside a single transaction.
     */
    public boolean approveLeaveRequest(int requestId, String comments) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // 1. Fetch request details
            String selectQuery = "SELECT employee_id, leave_type, start_date, end_date FROM leave_request WHERE id = ? AND status = 'Pending' FOR UPDATE";
            int employeeId = -1;
            String leaveType = null;
            Date startDate = null;
            Date endDate = null;

            try (PreparedStatement psSelect = conn.prepareStatement(selectQuery)) {
                psSelect.setInt(1, requestId);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        employeeId = rs.getInt("employee_id");
                        leaveType = rs.getString("leave_type");
                        startDate = rs.getDate("start_date");
                        endDate = rs.getDate("end_date");
                    } else {
                        conn.rollback();
                        return false; // Request not found or not Pending
                    }
                }
            }

            // Calculate leave days
            long diffMs = endDate.getTime() - startDate.getTime();
            int leaveDays = (int) (diffMs / (1000 * 60 * 60 * 24)) + 1;
            if (leaveDays <= 0) {
                conn.rollback();
                return false;
            }

            // Map leave type to balance column
            String column = null;
            if ("Casual Leave".equalsIgnoreCase(leaveType)) {
                column = "casual_leave";
            } else if ("Sick Leave".equalsIgnoreCase(leaveType)) {
                column = "sick_leave";
            } else if ("Earned Leave".equalsIgnoreCase(leaveType)) {
                column = "earned_leave";
            }

            if (column == null) {
                conn.rollback();
                return false; // Invalid leave type
            }

            // 2. Fetch current balance
            String balanceQuery = "SELECT " + column + " FROM leave_balance WHERE employee_id = ? FOR UPDATE";
            int currentBalance = -1;
            try (PreparedStatement psBalance = conn.prepareStatement(balanceQuery)) {
                psBalance.setInt(1, employeeId);
                try (ResultSet rs = psBalance.executeQuery()) {
                    if (rs.next()) {
                        currentBalance = rs.getInt(column);
                    }
                }
            }

            if (currentBalance < leaveDays) {
                conn.rollback();
                return false; // Insufficient leave balance
            }

            // 3. Deduct balance
            String updateBalanceQuery = "UPDATE leave_balance SET " + column + " = " + column + " - ? WHERE employee_id = ?";
            try (PreparedStatement psUpdateBalance = conn.prepareStatement(updateBalanceQuery)) {
                psUpdateBalance.setInt(1, leaveDays);
                psUpdateBalance.setInt(2, employeeId);
                psUpdateBalance.executeUpdate();
            }

            // 4. Update request status to Approved
            String updateRequestQuery = "UPDATE leave_request SET status = 'Approved', comments = ? WHERE id = ?";
            try (PreparedStatement psUpdateRequest = conn.prepareStatement(updateRequestQuery)) {
                psUpdateRequest.setString(1, comments);
                psUpdateRequest.setInt(2, requestId);
                psUpdateRequest.executeUpdate();
            }

            conn.commit(); // Commit Transaction
            return true;
        } catch (SQLException e) {
            System.err.println("SQL error during approveLeaveRequest. Rolling back.");
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
     * Gets count of leave requests by status (for dashboard stats).
     */
    public int getCountByStatus(String status) {
        String query = "SELECT COUNT(*) FROM leave_request WHERE status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Gets count of leave requests applied in the current calendar month.
     */
    public int getRequestsInCurrentMonth() {
        String query = "SELECT COUNT(*) FROM leave_request WHERE MONTH(applied_date) = MONTH(CURRENT_DATE()) AND YEAR(applied_date) = YEAR(CURRENT_DATE())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("SQL error in getRequestsInCurrentMonth");
            e.printStackTrace();
        }
        return 0;
    }

    private LeaveRequest mapResultSetToLeaveRequest(ResultSet rs) throws SQLException {
        LeaveRequest req = new LeaveRequest(
            rs.getInt("id"),
            rs.getInt("employee_id"),
            rs.getString("leave_type"),
            rs.getDate("start_date"),
            rs.getDate("end_date"),
            rs.getString("reason"),
            rs.getString("status"),
            rs.getTimestamp("applied_date"),
            rs.getString("comments")
        );
        req.setEmployeeName(rs.getString("employee_name"));
        return req;
    }
}
