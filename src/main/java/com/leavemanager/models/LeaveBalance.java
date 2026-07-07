package com.leavemanager.models;

public class LeaveBalance {
    private int employeeId;
    private int casualLeave;
    private int sickLeave;
    private int earnedLeave;

    public LeaveBalance() {}

    public LeaveBalance(int employeeId, int casualLeave, int sickLeave, int earnedLeave) {
        this.employeeId = employeeId;
        this.casualLeave = casualLeave;
        this.sickLeave = sickLeave;
        this.earnedLeave = earnedLeave;
    }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public int getCasualLeave() { return casualLeave; }
    public void setCasualLeave(int casualLeave) { this.casualLeave = casualLeave; }

    public int getSickLeave() { return sickLeave; }
    public void setSickLeave(int sickLeave) { this.sickLeave = sickLeave; }

    public int getEarnedLeave() { return earnedLeave; }
    public void setEarnedLeave(int earnedLeave) { this.earnedLeave = earnedLeave; }
}
