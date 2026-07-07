package com.leavemanager.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.leavemanager.dao.LeaveBalanceDAO;
import com.leavemanager.dao.LeaveRequestDAO;
import com.leavemanager.models.Employee;
import com.leavemanager.models.LeaveBalance;
import com.leavemanager.models.LeaveRequest;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.List;

public class EmployeeDashboard extends JFrame {
    private final Employee employee;
    private final LeaveBalanceDAO leaveBalanceDAO = new LeaveBalanceDAO();
    private final LeaveRequestDAO leaveRequestDAO = new LeaveRequestDAO();

    private JLabel lblCasualBalance;
    private JLabel lblSickBalance;
    private JLabel lblEarnedBalance;
    private JTable tblHistory;
    private DefaultTableModel tableModel;

    public EmployeeDashboard(Employee employee) {
        this.employee = employee;
        
        setTitle("Employee Dashboard - " + employee.getName());
        setSize(950, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        initUI();
        refreshDashboard();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // 1. Top Panel (Header and Logout)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JPanel welcomeTextPanel = new JPanel(new GridLayout(2, 1, 3, 3));
        JLabel lblWelcome = new JLabel("Welcome back, " + employee.getName() + "!", JLabel.LEFT);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblWelcome.setForeground(new Color(30, 80, 150));
        
        JLabel lblSub = new JLabel("Hindalco Industries Ltd. | Employee Portal", JLabel.LEFT);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(Color.GRAY);
        welcomeTextPanel.add(lblWelcome);
        welcomeTextPanel.add(lblSub);
        
        topPanel.add(welcomeTextPanel, BorderLayout.WEST);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setPreferredSize(new Dimension(100, 35));
        btnLogout.addActionListener(this::handleLogout);
        topPanel.add(btnLogout, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Split Pane to divide Profile (Left) and Content (Right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(260);
        splitPane.setDividerSize(5);
        splitPane.setEnabled(false); // Disable resizing to keep clean layout

        // 2. Left Panel (Profile details)
        JPanel profilePanel = new JPanel(new BorderLayout());
        profilePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel detailsGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;

        JLabel lblProfileTitle = new JLabel("My Profile Summary");
        lblProfileTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblProfileTitle.setForeground(new Color(30, 80, 150));
        lblProfileTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        gbc.gridy = 0;
        detailsGrid.add(lblProfileTitle, gbc);

        gbc.gridy = 1;
        detailsGrid.add(createDetailLabel("Employee ID:", String.valueOf(employee.getId())), gbc);

        gbc.gridy = 2;
        detailsGrid.add(createDetailLabel("Username:", employee.getUsername()), gbc);

        gbc.gridy = 3;
        detailsGrid.add(createDetailLabel("Email:", employee.getEmail()), gbc);

        gbc.gridy = 4;
        detailsGrid.add(createDetailLabel("Department:", employee.getDepartment()), gbc);

        gbc.gridy = 5;
        detailsGrid.add(createDetailLabel("Designation:", employee.getDesignation()), gbc);

        gbc.gridy = 6;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        detailsGrid.add(createDetailLabel("Joining Date:", df.format(employee.getJoiningDate())), gbc);

        profilePanel.add(detailsGrid, BorderLayout.NORTH);

        // Apply Leave button at bottom of profile
        JButton btnApplyLeave = new JButton("Apply for Leave");
        btnApplyLeave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnApplyLeave.putClientProperty("JButton.buttonType", "accent");
        btnApplyLeave.setPreferredSize(new Dimension(0, 45));
        btnApplyLeave.addActionListener(e -> handleApplyLeave());
        profilePanel.add(btnApplyLeave, BorderLayout.SOUTH);

        splitPane.setLeftComponent(profilePanel);

        // 3. Right Panel (Balances & History)
        JPanel contentPanel = new JPanel(new BorderLayout(0, 15));
        contentPanel.setBorder(new EmptyBorder(10, 15, 10, 10));

        // Balances Panel
        JPanel balanceContainer = new JPanel(new GridLayout(1, 3, 15, 0));
        balanceContainer.setPreferredSize(new Dimension(0, 110));

        balanceContainer.add(createBalanceCard("Casual Leave", lblCasualBalance = new JLabel("0"), new Color(240, 248, 255), new Color(30, 80, 150)));
        balanceContainer.add(createBalanceCard("Sick Leave", lblSickBalance = new JLabel("0"), new Color(245, 255, 250), new Color(46, 139, 87)));
        balanceContainer.add(createBalanceCard("Earned Leave", lblEarnedBalance = new JLabel("0"), new Color(255, 250, 240), new Color(184, 134, 11)));

        contentPanel.add(balanceContainer, BorderLayout.NORTH);

        // Leave History Panel
        JPanel historyPanel = new JPanel(new BorderLayout());
        JLabel lblHistoryTitle = new JLabel("Leave History");
        lblHistoryTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblHistoryTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        historyPanel.add(lblHistoryTitle, BorderLayout.NORTH);

        String[] columns = {"Leave Type", "Start Date", "End Date", "Days", "Status", "Reason", "Admin Comments"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblHistory = new JTable(tableModel);
        tblHistory.setRowHeight(25);
        tblHistory.getTableHeader().setReorderingAllowed(false);
        
        JScrollPane scrollPane = new JScrollPane(tblHistory);
        historyPanel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(historyPanel, BorderLayout.CENTER);
        splitPane.setRightComponent(contentPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    private JPanel createDetailLabel(String title, String value) {
        JPanel panel = new JPanel(new GridLayout(2, 1, 2, 2));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(Color.GRAY);
        
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        panel.add(lblTitle);
        panel.add(lblValue);
        return panel;
    }

    private JPanel createBalanceCard(String title, JLabel balanceLabel, Color bgColor, Color textColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createLineBorder(textColor.brighter(), 1, true));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        card.setBorder(BorderFactory.createCompoundBorder(
            card.getBorder(),
            new EmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(textColor);

        balanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        balanceLabel.setForeground(textColor);

        JLabel lblDays = new JLabel("Days Available");
        lblDays.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblDays.setForeground(Color.GRAY);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(balanceLabel, BorderLayout.CENTER);
        card.add(lblDays, BorderLayout.SOUTH);

        return card;
    }

    private void handleApplyLeave() {
        ApplyLeaveDialog dialog = new ApplyLeaveDialog(this, employee);
        dialog.setVisible(true);
        if (dialog.isSubmitted()) {
            refreshDashboard();
        }
    }

    private void refreshDashboard() {
        // Refresh Balances
        LeaveBalance balance = leaveBalanceDAO.getLeaveBalance(employee.getId());
        if (balance != null) {
            lblCasualBalance.setText(String.valueOf(balance.getCasualLeave()));
            lblSickBalance.setText(String.valueOf(balance.getSickLeave()));
            lblEarnedBalance.setText(String.valueOf(balance.getEarnedLeave()));
        } else {
            lblCasualBalance.setText("N/A");
            lblSickBalance.setText("N/A");
            lblEarnedBalance.setText("N/A");
        }

        // Refresh History Table
        tableModel.setRowCount(0);
        List<LeaveRequest> history = leaveRequestDAO.getLeaveHistoryByEmployee(employee.getId());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (LeaveRequest req : history) {
            long diffMs = req.getEndDate().getTime() - req.getStartDate().getTime();
            int days = (int) (diffMs / (1000 * 60 * 60 * 24)) + 1;
            
            Object[] row = {
                req.getLeaveType(),
                dateFormat.format(req.getStartDate()),
                dateFormat.format(req.getEndDate()),
                days,
                req.getStatus(),
                req.getReason(),
                req.getComments() != null ? req.getComments() : ""
            };
            tableModel.addRow(row);
        }
    }

    private void handleLogout(ActionEvent e) {
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            this.dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
