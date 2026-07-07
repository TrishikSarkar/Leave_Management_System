package com.leavemanager.models;

import java.sql.Date;
import java.sql.Timestamp;

public class LeaveRequest {
    private int id;
    private int employeeId;
    private String employeeName; // For display purposes in UI tables (joined from employee table)
    private String leaveType;
    private Date startDate;
    private Date endDate;
    private String reason;
    private String status;
    private Timestamp appliedDate;
    private String comments;

    public LeaveRequest() {}

    public LeaveRequest(int id, int employeeId, String leaveType, Date startDate, Date endDate, String reason, String status, Timestamp appliedDate, String comments) {
        this.id = id;
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
        this.appliedDate = appliedDate;
        this.comments = comments;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getAppliedDate() { return appliedDate; }
    public void setAppliedDate(Timestamp appliedDate) { this.appliedDate = appliedDate; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
