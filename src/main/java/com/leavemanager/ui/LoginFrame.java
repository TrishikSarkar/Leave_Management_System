package com.leavemanager.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.leavemanager.dao.AdminDAO;
import com.leavemanager.dao.EmployeeDAO;
import com.leavemanager.models.Admin;
import com.leavemanager.models.Employee;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JRadioButton rbtnEmployee;
    private JRadioButton rbtnAdmin;
    private JButton btnLogin;
    private JButton btnExit;

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    public LoginFrame() {
        setTitle("Hindalco Industries - Leave Management Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Header Panel
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel lblOrg = new JLabel("Hindalco Industries Ltd.", JLabel.CENTER);
        lblOrg.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblOrg.setForeground(new Color(30, 80, 150)); // Corporate Blue
        
        JLabel lblTitle = new JLabel("Employee Leave Management System", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitle.setForeground(Color.GRAY);

        headerPanel.add(lblOrg);
        headerPanel.add(lblTitle);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblUser, gbc);

        gbc.gridy = 1;
        txtUsername = new JTextField();
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter your username");
        txtUsername.setPreferredSize(new Dimension(0, 35));
        formPanel.add(txtUsername, gbc);

        // Password
        gbc.gridy = 2;
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblPass, gbc);

        gbc.gridy = 3;
        txtPassword = new JPasswordField();
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter your password");
        txtPassword.setPreferredSize(new Dimension(0, 35));
        formPanel.add(txtPassword, gbc);

        // Role Selection
        gbc.gridy = 4;
        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rbtnEmployee = new JRadioButton("Employee", true);
        rbtnAdmin = new JRadioButton("Administrator");
        
        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(rbtnEmployee);
        roleGroup.add(rbtnAdmin);

        rolePanel.add(rbtnEmployee);
        rolePanel.add(Box.createHorizontalStrut(20));
        rolePanel.add(rbtnAdmin);
        formPanel.add(rolePanel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        btnLogin = new JButton("Login");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.putClientProperty("JButton.buttonType", "accent");
        btnLogin.setPreferredSize(new Dimension(0, 40));
        btnLogin.addActionListener(this::handleLogin);

        btnExit = new JButton("Exit");
        btnExit.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnExit.setPreferredSize(new Dimension(0, 40));
        btnExit.addActionListener(e -> System.exit(0));

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnExit);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void handleLogin(ActionEvent e) {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        // Basic Validation
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Authentication logic running on Swing worker or directly (since local DB is extremely fast)
        if (rbtnEmployee.isSelected()) {
            Employee employee = employeeDAO.authenticate(username, password);
            if (employee != null) {
                // Success: open employee dashboard
                JOptionPane.showMessageDialog(this, "Welcome back, " + employee.getName() + "!", "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                openEmployeeDashboard(employee);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid employee credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                btnLogin.setEnabled(true);
            }
        } else {
            Admin admin = adminDAO.authenticate(username, password);
            if (admin != null) {
                // Success: open administrator dashboard
                JOptionPane.showMessageDialog(this, "Welcome, Administrator " + admin.getName() + "!", "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                openAdminDashboard(admin);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid administrator credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                btnLogin.setEnabled(true);
            }
        }
        setCursor(Cursor.getDefaultCursor());
    }

    private void openEmployeeDashboard(Employee employee) {
        this.dispose();
        new EmployeeDashboard(employee).setVisible(true);
    }

    private void openAdminDashboard(Admin admin) {
        this.dispose();
        new AdminDashboard(admin).setVisible(true);
    }



    public static void main(String[] args) {
        // Setup FlatLaf
        FlatIntelliJLaf.setup();
        
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
