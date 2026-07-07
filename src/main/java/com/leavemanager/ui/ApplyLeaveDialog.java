package com.leavemanager.ui;

import com.leavemanager.dao.LeaveBalanceDAO;
import com.leavemanager.dao.LeaveRequestDAO;
import com.leavemanager.models.Employee;
import com.leavemanager.models.LeaveBalance;
import com.leavemanager.models.LeaveRequest;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Date;
import java.util.Calendar;

public class ApplyLeaveDialog extends JDialog {
    private JComboBox<String> cbLeaveType;
    private JSpinner spinStartDate;
    private JSpinner spinEndDate;
    private JTextArea taReason;
    private JButton btnSubmit;
    private JButton btnCancel;

    private final Employee employee;
    private final LeaveRequestDAO leaveRequestDAO = new LeaveRequestDAO();
    private final LeaveBalanceDAO leaveBalanceDAO = new LeaveBalanceDAO();
    private boolean isSubmitted = false;

    public ApplyLeaveDialog(Frame parent, Employee employee) {
        super(parent, "Apply for Leave", true);
        this.employee = employee;
        
        setSize(450, 450);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Form Layout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Leave Type
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblType = new JLabel("Leave Type");
        lblType.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblType, gbc);

        gbc.gridy = 1;
        cbLeaveType = new JComboBox<>(new String[]{"Casual Leave", "Sick Leave", "Earned Leave"});
        cbLeaveType.setPreferredSize(new Dimension(0, 35));
        formPanel.add(cbLeaveType, gbc);

        // Start Date
        gbc.gridy = 2;
        JLabel lblStart = new JLabel("Start Date");
        lblStart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblStart, gbc);

        gbc.gridy = 3;
        spinStartDate = createDateSpinner();
        JPanel pnlStart = new JPanel(new BorderLayout(5, 0));
        JButton btnStartCal = new JButton("📅");
        btnStartCal.setMargin(new Insets(2, 5, 2, 5));
        btnStartCal.addActionListener(e -> {
            CalendarPickerDialog dialog = new CalendarPickerDialog(this, spinStartDate);
            dialog.setVisible(true);
        });
        pnlStart.add(spinStartDate, BorderLayout.CENTER);
        pnlStart.add(btnStartCal, BorderLayout.EAST);
        formPanel.add(pnlStart, gbc);

        // End Date
        gbc.gridy = 4;
        JLabel lblEnd = new JLabel("End Date");
        lblEnd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblEnd, gbc);

        gbc.gridy = 5;
        spinEndDate = createDateSpinner();
        JPanel pnlEnd = new JPanel(new BorderLayout(5, 0));
        JButton btnEndCal = new JButton("📅");
        btnEndCal.setMargin(new Insets(2, 5, 2, 5));
        btnEndCal.addActionListener(e -> {
            CalendarPickerDialog dialog = new CalendarPickerDialog(this, spinEndDate);
            dialog.setVisible(true);
        });
        pnlEnd.add(spinEndDate, BorderLayout.CENTER);
        pnlEnd.add(btnEndCal, BorderLayout.EAST);
        formPanel.add(pnlEnd, gbc);

        // Reason
        gbc.gridy = 6;
        JLabel lblReason = new JLabel("Reason for Leave");
        lblReason.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblReason, gbc);

        gbc.gridy = 7;
        taReason = new JTextArea(4, 20);
        taReason.setLineWrap(true);
        taReason.setWrapStyleWord(true);
        taReason.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane scrollReason = new JScrollPane(taReason);
        formPanel.add(scrollReason, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Actions
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        btnSubmit = new JButton("Submit Request");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSubmit.putClientProperty("JButton.buttonType", "accent");
        btnSubmit.setPreferredSize(new Dimension(140, 35));
        btnSubmit.addActionListener(e -> handleSubmit());

        btnCancel = new JButton("Cancel");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setPreferredSize(new Dimension(90, 35));
        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnSubmit);
        buttonPanel.add(btnCancel);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JSpinner createDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel(new java.util.Date(), null, null, Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        spinner.setPreferredSize(new Dimension(0, 35));
        return spinner;
    }

    private void handleSubmit() {
        String leaveType = (String) cbLeaveType.getSelectedItem();
        java.util.Date uStartDate = (java.util.Date) spinStartDate.getValue();
        java.util.Date uEndDate = (java.util.Date) spinEndDate.getValue();
        String reason = taReason.getText().trim();

        // 1. Validation: Dates are not null and End Date >= Start Date
        if (uStartDate == null || uEndDate == null) {
            JOptionPane.showMessageDialog(this, "Please select valid start and end dates.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Standardize dates to remove hours/minutes/seconds for day calculation
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(uStartDate);
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(uEndDate);
        calEnd.set(Calendar.HOUR_OF_DAY, 0);
        calEnd.set(Calendar.MINUTE, 0);
        calEnd.set(Calendar.SECOND, 0);
        calEnd.set(Calendar.MILLISECOND, 0);

        if (calEnd.before(calStart)) {
            JOptionPane.showMessageDialog(this, "End Date cannot be before Start Date.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Validation: Reason is not empty
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a reason for your leave request.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            taReason.requestFocus();
            return;
        }

        // Calculate requested days
        long diffMs = calEnd.getTimeInMillis() - calStart.getTimeInMillis();
        int requestedDays = (int) (diffMs / (1000 * 60 * 60 * 24)) + 1;

        // 3. Check leave balance in database
        LeaveBalance balance = leaveBalanceDAO.getLeaveBalance(employee.getId());
        if (balance == null) {
            JOptionPane.showMessageDialog(this, "Failed to retrieve your leave balance from database.", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int availableBalance = 0;
        if ("Casual Leave".equalsIgnoreCase(leaveType)) {
            availableBalance = balance.getCasualLeave();
        } else if ("Sick Leave".equalsIgnoreCase(leaveType)) {
            availableBalance = balance.getSickLeave();
        } else if ("Earned Leave".equalsIgnoreCase(leaveType)) {
            availableBalance = balance.getEarnedLeave();
        }

        if (availableBalance < requestedDays) {
            JOptionPane.showMessageDialog(this, 
                String.format("Insufficient leave balance!\nRequested: %d days\nAvailable %s: %d days", 
                    requestedDays, leaveType, availableBalance), 
                "Insufficient Balance", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 4. Create and Insert Leave Request
        Date sqlStartDate = new Date(calStart.getTimeInMillis());
        Date sqlEndDate = new Date(calEnd.getTimeInMillis());

        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(employee.getId());
        request.setLeaveType(leaveType);
        request.setStartDate(sqlStartDate);
        request.setEndDate(sqlEndDate);
        request.setReason(reason);

        boolean success = leaveRequestDAO.insertLeaveRequest(request);
        if (success) {
            JOptionPane.showMessageDialog(this, "Leave request submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            isSubmitted = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to submit leave request. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSubmitted() {
        return isSubmitted;
    }
}
