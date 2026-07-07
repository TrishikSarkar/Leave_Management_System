package com.leavemanager.models;

import java.sql.Date;

public class Employee {
    private int id;
    private String username;
    private String password;
    private String name;
    private String email;
    private String department;
    private String designation;
    private Date joiningDate;

    public Employee() {}

    public Employee(int id, String username, String password, String name, String email, String department, String designation, Date joiningDate) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.department = department;
        this.designation = designation;
        this.joiningDate = joiningDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Date getJoiningDate() { return joiningDate; }
    public void setJoiningDate(Date joiningDate) { this.joiningDate = joiningDate; }
}
