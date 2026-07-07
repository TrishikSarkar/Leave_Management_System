package com.leavemanager.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.leavemanager.dao.EmployeeDAO;
import com.leavemanager.dao.LeaveBalanceDAO;
import com.leavemanager.dao.LeaveRequestDAO;
import com.leavemanager.models.Admin;
import com.leavemanager.models.Employee;
import com.leavemanager.models.LeaveBalance;
import com.leavemanager.models.LeaveRequest;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import com.leavemanager.database.DatabaseConnection;

public class AdminDashboard extends JFrame {
    private final Admin admin;
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final LeaveRequestDAO leaveRequestDAO = new LeaveRequestDAO();
    private final LeaveBalanceDAO leaveBalanceDAO = new LeaveBalanceDAO();

    // Stats Labels
    private JLabel lblTotalEmployees;
    private JLabel lblPendingRequests;
    private JLabel lblApprovedLeaves;
    private JLabel lblRejectedLeaves;
    private JLabel lblMonthlyStats;

    // Overview Components
    private JTable tblPendingRequests;
    private DefaultTableModel pendingModel;
    private JTextArea taReviewComments;

    // Employee Management Components
    private JTable tblEmployees;
    private DefaultTableModel employeeModel;
    private JTextField txtSearch;
    private JTextField txtEmpUsername;
    private JPasswordField txtEmpPassword; // Editable on insert, optional on update
    private JTextField txtEmpName;
    private JTextField txtEmpEmail;
    private JTextField txtEmpDept;
    private JTextField txtEmpDesg;
    private JSpinner spinEmpJoining;
    
    // Leave Balance Editor (for selected employee)
    private JSpinner spinCasual;
    private JSpinner spinSick;
    private JSpinner spinEarned;
    private JLabel lblSelectedEmpBalanceTitle;
    private Employee selectedEmployeeForBalance;

    private JButton btnAddEmployee;
    private JButton btnUpdateEmployee;
    private JButton btnDeleteEmployee;
    private JButton btnSaveBalance;

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        
        setTitle("Administrator Dashboard - " + admin.getName());
        setSize(1050, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        refreshAllData();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // 1. Header Pane
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JPanel welcomeTextPanel = new JPanel(new GridLayout(2, 1, 3, 3));
        JLabel lblWelcome = new JLabel("Welcome, Administrator " + admin.getName(), JLabel.LEFT);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblWelcome.setForeground(new Color(30, 80, 150));
        
        JLabel lblSub = new JLabel("Hindalco Industries Ltd. | Management Control Center", JLabel.LEFT);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(Color.GRAY);
        welcomeTextPanel.add(lblWelcome);
        welcomeTextPanel.add(lblSub);
        
        headerPanel.add(welcomeTextPanel, BorderLayout.WEST);

        JPanel topActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topActionPanel.setOpaque(false);

        JButton btnClearDb = new JButton("Clear Database");
        btnClearDb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearDb.setPreferredSize(new Dimension(130, 35));
        btnClearDb.putClientProperty("JButton.buttonType", "round");
        btnClearDb.addActionListener(this::handleClearDatabase);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setPreferredSize(new Dimension(100, 35));
        btnLogout.addActionListener(this::handleLogout);

        topActionPanel.add(btnClearDb);
        topActionPanel.add(btnLogout);
        headerPanel.add(topActionPanel, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. Tabbed Panel
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Overview & Stats
        tabbedPane.addTab("Dashboard Overview", createOverviewTab());

        // Tab 2: Manage Employees
        tabbedPane.addTab("Manage Employees & Balances", createEmployeeTab());

        // Tab 3: PDF / Excel Reports
        tabbedPane.addTab("Generate Reports", createReportsTab());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    // --- TAB 1: OVERVIEW & STATS ---
    private JPanel createOverviewTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Stats Cards Panel
        JPanel statsPanel = new JPanel(new GridLayout(1, 5, 12, 0));
        statsPanel.setPreferredSize(new Dimension(0, 100));

        statsPanel.add(createStatCard("Total Employees", lblTotalEmployees = new JLabel("0"), new Color(30, 80, 150)));
        statsPanel.add(createStatCard("Pending Requests", lblPendingRequests = new JLabel("0"), new Color(245, 166, 35)));
        statsPanel.add(createStatCard("Approved Leaves", lblApprovedLeaves = new JLabel("0"), new Color(46, 139, 87)));
        statsPanel.add(createStatCard("Rejected Leaves", lblRejectedLeaves = new JLabel("0"), new Color(208, 2, 27)));
        statsPanel.add(createStatCard("Monthly Requests", lblMonthlyStats = new JLabel("0"), new Color(114, 101, 230)));

        panel.add(statsPanel, BorderLayout.NORTH);

        // Operations Panel: Left (Table of Pending), Right (Approve/Reject review comments form)
        JSplitPane splitOps = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitOps.setDividerLocation(650);
        splitOps.setDividerSize(5);
        splitOps.setEnabled(false);

        // Table Panel (Left)
        JPanel tableContainer = new JPanel(new BorderLayout());
        JLabel lblPendingTitle = new JLabel("Pending Leave Requests");
        lblPendingTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblPendingTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        tableContainer.add(lblPendingTitle, BorderLayout.NORTH);

        String[] cols = {"ID", "Employee Name", "Type", "Start Date", "End Date", "Days", "Reason"};
        pendingModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblPendingRequests = new JTable(pendingModel);
        tblPendingRequests.setRowHeight(25);
        tblPendingRequests.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollTable = new JScrollPane(tblPendingRequests);
        tableContainer.add(scrollTable, BorderLayout.CENTER);
        
        splitOps.setLeftComponent(tableContainer);

        // Review Form Panel (Right)
        JPanel reviewPanel = new JPanel(new GridBagLayout());
        reviewPanel.setBorder(new EmptyBorder(0, 15, 0, 5));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridx = 0;

        JLabel lblReviewTitle = new JLabel("Review Leave Action");
        lblReviewTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblReviewTitle.setForeground(new Color(30, 80, 150));
        lblReviewTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        gbc.gridy = 0;
        reviewPanel.add(lblReviewTitle, gbc);

        gbc.gridy = 1;
        JLabel lblComments = new JLabel("Admin Comments / Remarks");
        lblComments.setFont(new Font("Segoe UI", Font.BOLD, 11));
        reviewPanel.add(lblComments, gbc);

        gbc.gridy = 2;
        taReviewComments = new JTextArea(4, 15);
        taReviewComments.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        taReviewComments.setLineWrap(true);
        taReviewComments.setWrapStyleWord(true);
        reviewPanel.add(new JScrollPane(taReviewComments), gbc);

        gbc.gridy = 3;
        JButton btnApprove = new JButton("Approve Request");
        btnApprove.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnApprove.putClientProperty("JButton.buttonType", "accent");
        btnApprove.setPreferredSize(new Dimension(0, 38));
        btnApprove.addActionListener(e -> handleApproveLeave());
        reviewPanel.add(btnApprove, gbc);

        gbc.gridy = 4;
        JButton btnReject = new JButton("Reject Request");
        btnReject.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnReject.setPreferredSize(new Dimension(0, 38));
        btnReject.addActionListener(e -> handleRejectLeave());
        reviewPanel.add(btnReject, gbc);

        splitOps.setRightComponent(reviewPanel);
        panel.add(splitOps, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        card.setBorder(BorderFactory.createCompoundBorder(
            card.getBorder(),
            new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(Color.GRAY);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(accentColor);

        JPanel underline = new JPanel();
        underline.setPreferredSize(new Dimension(0, 3));
        underline.setBackground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(underline, BorderLayout.SOUTH);

        return card;
    }

    // --- TAB 2: MANAGE EMPLOYEES & BALANCES ---
    private JPanel createEmployeeTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Split Directory (Left JTable + Search) and Editor (Right Forms)
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(520);
        split.setDividerSize(5);
        split.setEnabled(false);

        // Employee Directory (Left Component)
        JPanel dirPanel = new JPanel(new BorderLayout(0, 10));
        
        // Search bar
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtSearch = new JTextField();
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter name, dept, or desg...");
        txtSearch.setPreferredSize(new Dimension(0, 32));
        txtSearch.addActionListener(e -> handleSearchEmployees());
        
        JButton btnClearSearch = new JButton("Reset");
        btnClearSearch.setPreferredSize(new Dimension(80, 32));
        btnClearSearch.addActionListener(e -> {
            txtSearch.setText("");
            loadAllEmployees();
        });

        searchPanel.add(lblSearch, BorderLayout.WEST);
        searchPanel.add(txtSearch, BorderLayout.CENTER);
        searchPanel.add(btnClearSearch, BorderLayout.EAST);
        dirPanel.add(searchPanel, BorderLayout.NORTH);

        // Employee Table
        String[] cols = {"ID", "Username", "Name", "Department", "Designation"};
        employeeModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblEmployees = new JTable(employeeModel);
        tblEmployees.setRowHeight(25);
        tblEmployees.getSelectionModel().addListSelectionListener(e -> handleEmployeeRowSelection());
        JScrollPane scrollTable = new JScrollPane(tblEmployees);
        dirPanel.add(scrollTable, BorderLayout.CENTER);

        split.setLeftComponent(dirPanel);

        // Editors Panel (Right Component)
        JPanel editorsContainer = new JPanel();
        editorsContainer.setLayout(new BoxLayout(editorsContainer, BoxLayout.Y_AXIS));
        editorsContainer.setBorder(new EmptyBorder(0, 10, 0, 5));

        // Form: Profile Editor
        JPanel profileForm = new JPanel(new GridBagLayout());
        profileForm.setBorder(BorderFactory.createTitledBorder("Employee Details Editor"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.weightx = 1.0;

        // Form Fields
        gbc.gridx = 0; gbc.gridy = 0;
        profileForm.add(new JLabel("Username"), gbc);
        gbc.gridx = 1;
        txtEmpUsername = new JTextField();
        profileForm.add(txtEmpUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        profileForm.add(new JLabel("Password"), gbc);
        gbc.gridx = 1;
        txtEmpPassword = new JPasswordField();
        txtEmpPassword.setToolTipText("Enter value to set/update password");
        profileForm.add(txtEmpPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        profileForm.add(new JLabel("Name"), gbc);
        gbc.gridx = 1;
        txtEmpName = new JTextField();
        profileForm.add(txtEmpName, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        profileForm.add(new JLabel("Email"), gbc);
        gbc.gridx = 1;
        txtEmpEmail = new JTextField();
        profileForm.add(txtEmpEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        profileForm.add(new JLabel("Department"), gbc);
        gbc.gridx = 1;
        txtEmpDept = new JTextField();
        profileForm.add(txtEmpDept, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        profileForm.add(new JLabel("Designation"), gbc);
        gbc.gridx = 1;
        txtEmpDesg = new JTextField();
        profileForm.add(txtEmpDesg, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        profileForm.add(new JLabel("Joining Date"), gbc);
        gbc.gridx = 1;
        spinEmpJoining = new JSpinner(new SpinnerDateModel(new java.util.Date(), null, null, Calendar.DAY_OF_MONTH));
        spinEmpJoining.setEditor(new JSpinner.DateEditor(spinEmpJoining, "yyyy-MM-dd"));
        
        JPanel pnlJoining = new JPanel(new BorderLayout(5, 0));
        JButton btnJoiningCal = new JButton("📅");
        btnJoiningCal.setMargin(new Insets(2, 5, 2, 5));
        btnJoiningCal.addActionListener(e -> {
            CalendarPickerDialog dialog = new CalendarPickerDialog(this, spinEmpJoining);
            dialog.setVisible(true);
        });
        pnlJoining.add(spinEmpJoining, BorderLayout.CENTER);
        pnlJoining.add(btnJoiningCal, BorderLayout.EAST);
        profileForm.add(pnlJoining, gbc);

        // Actions: Profile CRUD
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        JPanel profileActions = new JPanel(new GridLayout(1, 3, 5, 0));
        btnAddEmployee = new JButton("Add New");
        btnAddEmployee.addActionListener(this::handleAddEmployee);
        btnUpdateEmployee = new JButton("Update Selected");
        btnUpdateEmployee.addActionListener(this::handleUpdateEmployee);
        btnDeleteEmployee = new JButton("Delete");
        btnDeleteEmployee.addActionListener(this::handleDeleteEmployee);
        
        profileActions.add(btnAddEmployee);
        profileActions.add(btnUpdateEmployee);
        profileActions.add(btnDeleteEmployee);
        profileForm.add(profileActions, gbc);

        // Clear Selection / New Form button
        gbc.gridy = 8;
        JButton btnClearForm = new JButton("Clear Form / Select None");
        btnClearForm.addActionListener(e -> clearEmployeeFormSelection());
        profileForm.add(btnClearForm, gbc);

        editorsContainer.add(profileForm);
        editorsContainer.add(Box.createVerticalStrut(15));

        // Form: Leave Balance Editor
        JPanel balanceForm = new JPanel(new GridBagLayout());
        balanceForm.setBorder(BorderFactory.createTitledBorder("Manage Leave Balances"));
        GridBagConstraints gbcB = new GridBagConstraints();
        gbcB.fill = GridBagConstraints.HORIZONTAL;
        gbcB.insets = new Insets(6, 5, 6, 5);
        gbcB.weightx = 1.0;

        gbcB.gridx = 0; gbcB.gridy = 0; gbcB.gridwidth = 2;
        lblSelectedEmpBalanceTitle = new JLabel("Select an employee to manage balances", JLabel.CENTER);
        lblSelectedEmpBalanceTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSelectedEmpBalanceTitle.setForeground(Color.GRAY);
        balanceForm.add(lblSelectedEmpBalanceTitle, gbcB);

        gbcB.gridwidth = 1;
        gbcB.gridx = 0; gbcB.gridy = 1;
        balanceForm.add(new JLabel("Casual Leave:"), gbcB);
        gbcB.gridx = 1;
        spinCasual = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        balanceForm.add(spinCasual, gbcB);

        gbcB.gridx = 0; gbcB.gridy = 2;
        balanceForm.add(new JLabel("Sick Leave:"), gbcB);
        gbcB.gridx = 1;
        spinSick = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        balanceForm.add(spinSick, gbcB);

        gbcB.gridx = 0; gbcB.gridy = 3;
        balanceForm.add(new JLabel("Earned Leave:"), gbcB);
        gbcB.gridx = 1;
        spinEarned = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        balanceForm.add(spinEarned, gbcB);

        gbcB.gridx = 0; gbcB.gridy = 4; gbcB.gridwidth = 2;
        btnSaveBalance = new JButton("Update Leave Balance");
        btnSaveBalance.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSaveBalance.putClientProperty("JButton.buttonType", "accent");
        btnSaveBalance.setEnabled(false);
        btnSaveBalance.addActionListener(this::handleSaveBalance);
        balanceForm.add(btnSaveBalance, gbcB);

        editorsContainer.add(balanceForm);

        JScrollPane rightScroll = new JScrollPane(editorsContainer);
        rightScroll.setBorder(BorderFactory.createEmptyBorder());
        rightScroll.getVerticalScrollBar().setUnitIncrement(12);
        split.setRightComponent(rightScroll);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    // --- TAB 3: REPORTS ---
    private JPanel createReportsTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;

        JLabel lblTitle = new JLabel("Generate Centralized Reports");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 80, 150));
        gbc.gridy = 0;
        panel.add(lblTitle, gbc);

        JLabel lblDesc = new JLabel("Export full logs of employee details and leave balances in PDF or Excel spreadsheet formats.");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDesc.setForeground(Color.GRAY);
        gbc.gridy = 1;
        panel.add(lblDesc, gbc);

        // Buttons
        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 30, 0));
        buttonRow.setPreferredSize(new Dimension(480, 130));

        JButton btnPDF = new JButton("Download PDF Report");
        btnPDF.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPDF.putClientProperty("JButton.buttonType", "accent");
        btnPDF.addActionListener(this::handleGeneratePDF);
        
        JButton btnExcel = new JButton("Download Excel Report");
        btnExcel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnExcel.addActionListener(this::handleGenerateExcel);

        buttonRow.add(btnPDF);
        buttonRow.add(btnExcel);

        gbc.gridy = 2;
        panel.add(buttonRow, gbc);

        return panel;
    }

    // --- EVENT HANDLERS & LOGIC ---

    private void refreshAllData() {
        refreshStats();
        loadPendingRequests();
        
        int selectedEmpId = -1;
        int selectedRow = tblEmployees.getSelectedRow();
        if (selectedRow != -1) {
            selectedEmpId = (int) employeeModel.getValueAt(selectedRow, 0);
        }
        
        loadAllEmployees();
        
        if (selectedEmpId != -1) {
            boolean found = false;
            for (int r = 0; r < tblEmployees.getRowCount(); r++) {
                if ((int) employeeModel.getValueAt(r, 0) == selectedEmpId) {
                    tblEmployees.setRowSelectionInterval(r, r);
                    found = true;
                    break;
                }
            }
            if (!found) {
                clearEmployeeFormSelection();
            }
        } else {
            clearEmployeeFormSelection();
        }
    }

    private void refreshStats() {
        lblTotalEmployees.setText(String.valueOf(employeeDAO.getEmployeeCount()));
        lblPendingRequests.setText(String.valueOf(leaveRequestDAO.getCountByStatus("Pending")));
        lblApprovedLeaves.setText(String.valueOf(leaveRequestDAO.getCountByStatus("Approved")));
        lblRejectedLeaves.setText(String.valueOf(leaveRequestDAO.getCountByStatus("Rejected")));
        lblMonthlyStats.setText(String.valueOf(leaveRequestDAO.getRequestsInCurrentMonth()));
    }

    private void loadPendingRequests() {
        pendingModel.setRowCount(0);
        List<LeaveRequest> list = leaveRequestDAO.getPendingLeaveRequests();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        for (LeaveRequest req : list) {
            long diffMs = req.getEndDate().getTime() - req.getStartDate().getTime();
            int days = (int) (diffMs / (1000 * 60 * 60 * 24)) + 1;
            
            Object[] row = {
                req.getId(),
                req.getEmployeeName(),
                req.getLeaveType(),
                df.format(req.getStartDate()),
                df.format(req.getEndDate()),
                days,
                req.getReason()
            };
            pendingModel.addRow(row);
        }
    }

    private void loadAllEmployees() {
        employeeModel.setRowCount(0);
        List<Employee> list = employeeDAO.getAllEmployees();
        for (Employee emp : list) {
            Object[] row = {
                emp.getId(),
                emp.getUsername(),
                emp.getName(),
                emp.getDepartment(),
                emp.getDesignation()
            };
            employeeModel.addRow(row);
        }
    }

    private void handleSearchEmployees() {
        String q = txtSearch.getText().trim();
        if (q.isEmpty()) {
            loadAllEmployees();
            return;
        }
        employeeModel.setRowCount(0);
        List<Employee> list = employeeDAO.searchEmployees(q);
        for (Employee emp : list) {
            Object[] row = {
                emp.getId(),
                emp.getUsername(),
                emp.getName(),
                emp.getDepartment(),
                emp.getDesignation()
            };
            employeeModel.addRow(row);
        }
    }

    private void handleEmployeeRowSelection() {
        int selectedRow = tblEmployees.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        int employeeId = (int) employeeModel.getValueAt(selectedRow, 0);
        Employee emp = employeeDAO.getEmployeeById(employeeId);
        if (emp != null) {
            selectedEmployeeForBalance = emp;
            
            // Populate Details Editor
            txtEmpUsername.setText(emp.getUsername());
            txtEmpPassword.setText(""); // Keep password field empty (update is optional)
            txtEmpName.setText(emp.getName());
            txtEmpEmail.setText(emp.getEmail());
            txtEmpDept.setText(emp.getDepartment());
            txtEmpDesg.setText(emp.getDesignation());
            spinEmpJoining.setValue(new java.util.Date(emp.getJoiningDate().getTime()));

            // Populate Balances Editor
            lblSelectedEmpBalanceTitle.setText("Balances for: " + emp.getName() + " (ID: " + emp.getId() + ")");
            LeaveBalance balance = leaveBalanceDAO.getLeaveBalance(employeeId);
            if (balance != null) {
                spinCasual.setValue(balance.getCasualLeave());
                spinSick.setValue(balance.getSickLeave());
                spinEarned.setValue(balance.getEarnedLeave());
            } else {
                spinCasual.setValue(0);
                spinSick.setValue(0);
                spinEarned.setValue(0);
            }
            btnSaveBalance.setEnabled(true);
            btnUpdateEmployee.setEnabled(true);
            btnDeleteEmployee.setEnabled(true);
        }
    }

    private void clearEmployeeFormSelection() {
        selectedEmployeeForBalance = null;
        tblEmployees.clearSelection();

        txtEmpUsername.setText("");
        txtEmpPassword.setText("");
        txtEmpName.setText("");
        txtEmpEmail.setText("");
        txtEmpDept.setText("");
        txtEmpDesg.setText("");
        spinEmpJoining.setValue(new java.util.Date());

        lblSelectedEmpBalanceTitle.setText("Select an employee to manage balances");
        spinCasual.setValue(0);
        spinSick.setValue(0);
        spinEarned.setValue(0);

        btnSaveBalance.setEnabled(false);
        btnUpdateEmployee.setEnabled(false);
        btnDeleteEmployee.setEnabled(false);
    }

    private void handleAddEmployee(ActionEvent e) {
        String username = txtEmpUsername.getText().trim();
        String password = new String(txtEmpPassword.getPassword()).trim();
        String name = txtEmpName.getText().trim();
        String email = txtEmpEmail.getText().trim();
        String dept = txtEmpDept.getText().trim();
        String desg = txtEmpDesg.getText().trim();
        java.util.Date joining = (java.util.Date) spinEmpJoining.getValue();

        // Validation
        if (username.isEmpty() || password.isEmpty() || name.isEmpty() || email.isEmpty() || dept.isEmpty() || desg.isEmpty() || joining == null) {
            JOptionPane.showMessageDialog(this, "All fields (including password) are required to add a new employee.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Employee emp = new Employee();
        emp.setUsername(username);
        emp.setPassword(password);
        emp.setName(name);
        emp.setEmail(email);
        emp.setDepartment(dept);
        emp.setDesignation(desg);
        emp.setJoiningDate(new Date(joining.getTime()));

        boolean success = employeeDAO.addEmployee(emp);
        if (success) {
            JOptionPane.showMessageDialog(this, "New employee added and balances initialized to 0 successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshAllData();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add employee. Username might already exist.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdateEmployee(ActionEvent e) {
        if (selectedEmployeeForBalance == null) return;

        String username = txtEmpUsername.getText().trim();
        String password = new String(txtEmpPassword.getPassword()).trim();
        String name = txtEmpName.getText().trim();
        String email = txtEmpEmail.getText().trim();
        String dept = txtEmpDept.getText().trim();
        String desg = txtEmpDesg.getText().trim();
        java.util.Date joining = (java.util.Date) spinEmpJoining.getValue();

        if (username.isEmpty() || name.isEmpty() || email.isEmpty() || dept.isEmpty() || desg.isEmpty() || joining == null) {
            JOptionPane.showMessageDialog(this, "Username, Name, Email, Department, Designation, and Joining Date are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        selectedEmployeeForBalance.setUsername(username);
        if (!password.isEmpty()) {
            selectedEmployeeForBalance.setPassword(password);
        } else {
            selectedEmployeeForBalance.setPassword(null); // Keep unchanged in DB
        }
        selectedEmployeeForBalance.setName(name);
        selectedEmployeeForBalance.setEmail(email);
        selectedEmployeeForBalance.setDepartment(dept);
        selectedEmployeeForBalance.setDesignation(desg);
        selectedEmployeeForBalance.setJoiningDate(new Date(joining.getTime()));

        boolean success = employeeDAO.updateEmployee(selectedEmployeeForBalance);
        if (success) {
            JOptionPane.showMessageDialog(this, "Employee details updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshAllData();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update employee details.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteEmployee(ActionEvent e) {
        if (selectedEmployeeForBalance == null) return;

        int choice = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete employee: " + selectedEmployeeForBalance.getName() + "?\nThis will delete all their leave records and balances.", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.WARNING_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            boolean success = employeeDAO.deleteEmployee(selectedEmployeeForBalance.getId());
            if (success) {
                JOptionPane.showMessageDialog(this, "Employee deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshAllData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete employee.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleSaveBalance(ActionEvent e) {
        if (selectedEmployeeForBalance == null) return;

        int casual = (int) spinCasual.getValue();
        int sick = (int) spinSick.getValue();
        int earned = (int) spinEarned.getValue();

        LeaveBalance balance = new LeaveBalance(selectedEmployeeForBalance.getId(), casual, sick, earned);
        boolean success = leaveBalanceDAO.updateLeaveBalance(balance);
        if (success) {
            JOptionPane.showMessageDialog(this, "Leave balances updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshAllData();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update leave balances.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleApproveLeave() {
        int selectedRow = tblPendingRequests.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a pending request from the table to approve.", "Action Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) pendingModel.getValueAt(selectedRow, 0);
        String comments = taReviewComments.getText().trim();

        boolean success = leaveRequestDAO.approveLeaveRequest(requestId, comments);
        if (success) {
            JOptionPane.showMessageDialog(this, "Leave request approved successfully, and employee leave balance has been updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
            taReviewComments.setText("");
            refreshAllData();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to approve leave request. Check if the employee has sufficient balance.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRejectLeave() {
        int selectedRow = tblPendingRequests.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a pending request from the table to reject.", "Action Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) pendingModel.getValueAt(selectedRow, 0);
        String comments = taReviewComments.getText().trim();

        boolean success = leaveRequestDAO.rejectLeaveRequest(requestId, comments);
        if (success) {
            JOptionPane.showMessageDialog(this, "Leave request rejected.", "Success", JOptionPane.INFORMATION_MESSAGE);
            taReviewComments.setText("");
            refreshAllData();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to reject leave request.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleGeneratePDF(ActionEvent e) {
        // Placeholders, will be linked to PDFReportGenerator in Phase 7
        try {
            com.leavemanager.reports.PDFReportGenerator.generateReport(this);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error generating PDF report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleGenerateExcel(ActionEvent e) {
        // Placeholders, will be linked to ExcelReportGenerator in Phase 7
        try {
            com.leavemanager.reports.ExcelReportGenerator.generateReport(this);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error generating Excel report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLogout(ActionEvent e) {
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            this.dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void handleClearDatabase(ActionEvent e) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear the entire database?\nThis will permanently delete all employee records, leave balances, and requests.",
                "Confirm Clear Database",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        int choice2 = JOptionPane.showConfirmDialog(this,
                "WARNING: This action is irreversible. All employee data and records will be lost.\nDo you want to proceed?",
                "Double Confirmation Required",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE);

        if (choice2 != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM leave_request");
                 PreparedStatement ps2 = conn.prepareStatement("DELETE FROM leave_balance");
                 PreparedStatement ps3 = conn.prepareStatement("DELETE FROM employee")) {
                
                int deletedRequests = ps1.executeUpdate();
                int deletedBalances = ps2.executeUpdate();
                int deletedEmployees = ps3.executeUpdate();
                
                conn.commit();
                
                JOptionPane.showMessageDialog(this,
                        "Database reset successfully!\nRecords deleted:\n" +
                        "- Leave Requests: " + deletedRequests + "\n" +
                        "- Leave Balances: " + deletedBalances + "\n" +
                        "- Employees: " + deletedEmployees,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                
                refreshAllData();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error resetting database:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
