package com.leavemanager.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;

public class CalendarPickerDialog extends JDialog {
    private final JSpinner targetSpinner;
    private Calendar currentCalendar;
    
    private JLabel lblMonthYear;
    private JPanel pnlDays;
    
    public CalendarPickerDialog(Window parent, JSpinner targetSpinner) {
        super(parent, "Select Date", ModalityType.APPLICATION_MODAL);
        this.targetSpinner = targetSpinner;
        
        Date initialDate = (Date) targetSpinner.getValue();
        currentCalendar = Calendar.getInstance();
        if (initialDate != null) {
            currentCalendar.setTime(initialDate);
        }
        
        setSize(340, 290);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());
        
        // 1. Navigation Panel (Top)
        JPanel pnlNav = new JPanel(new BorderLayout(5, 5));
        pnlNav.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JButton btnPrev = new JButton("<");
        JButton btnNext = new JButton(">");
        lblMonthYear = new JLabel("", JLabel.CENTER);
        lblMonthYear.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        btnPrev.addActionListener(e -> navigateMonth(-1));
        btnNext.addActionListener(e -> navigateMonth(1));
        
        pnlNav.add(btnPrev, BorderLayout.WEST);
        pnlNav.add(lblMonthYear, BorderLayout.CENTER);
        pnlNav.add(btnNext, BorderLayout.EAST);
        
        add(pnlNav, BorderLayout.NORTH);
        
        // 2. Days Panel (Center)
        JPanel pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Days of week header
        JPanel pnlWeekHeaders = new JPanel(new GridLayout(1, 7, 2, 2));
        pnlWeekHeaders.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : daysOfWeek) {
            JLabel lbl = new JLabel(day, JLabel.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setForeground(Color.GRAY);
            pnlWeekHeaders.add(lbl);
        }
        pnlCenter.add(pnlWeekHeaders, BorderLayout.NORTH);
        
        pnlDays = new JPanel(new GridLayout(6, 7, 2, 2));
        pnlCenter.add(pnlDays, BorderLayout.CENTER);
        
        add(pnlCenter, BorderLayout.CENTER);
        
        // 3. Footer (Bottom)
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlFooter.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        JButton btnToday = new JButton("Today");
        JButton btnCancel = new JButton("Cancel");
        
        btnToday.addActionListener(e -> {
            targetSpinner.setValue(new Date());
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());
        
        pnlFooter.add(btnToday);
        pnlFooter.add(btnCancel);
        add(pnlFooter, BorderLayout.SOUTH);
        
        updateCalendar();
    }
    
    private void navigateMonth(int amount) {
        currentCalendar.add(Calendar.MONTH, amount);
        updateCalendar();
    }
    
    private void updateCalendar() {
        // Month and Year label
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);
        String[] months = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        lblMonthYear.setText(months[month] + " " + year);
        
        pnlDays.removeAll();
        
        // Find first day of month and max days
        Calendar temp = (Calendar) currentCalendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK); // 1 = Sunday, 2 = Monday...
        int maxDays = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Selected day highlight check
        Calendar selectedCal = Calendar.getInstance();
        Date selDate = (Date) targetSpinner.getValue();
        if (selDate != null) {
            selectedCal.setTime(selDate);
        }
        boolean isCurrentMonth = (selectedCal.get(Calendar.YEAR) == year && selectedCal.get(Calendar.MONTH) == month);
        int selectedDay = isCurrentMonth ? selectedCal.get(Calendar.DAY_OF_MONTH) : -1;
        
        // Empty cells before start
        for (int i = 1; i < firstDayOfWeek; i++) {
            pnlDays.add(new JLabel(""));
        }
        
        // Day cells
        for (int day = 1; day <= maxDays; day++) {
            final int selectedDayVal = day;
            JButton btnDay = new JButton(String.valueOf(day));
            btnDay.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnDay.setMargin(new Insets(2, 2, 2, 2));
            
            // Highlight currently selected day
            if (day == selectedDay) {
                btnDay.putClientProperty("JButton.buttonType", "accent");
            }
            
            btnDay.addActionListener(e -> {
                Calendar res = (Calendar) currentCalendar.clone();
                res.set(Calendar.DAY_OF_MONTH, selectedDayVal);
                targetSpinner.setValue(res.getTime());
                dispose();
            });
            
            pnlDays.add(btnDay);
        }
        
        // Empty cells after end
        int totalCells = (firstDayOfWeek - 1) + maxDays;
        int remainder = 42 - totalCells; // 6 rows * 7 columns = 42
        for (int i = 0; i < remainder; i++) {
            pnlDays.add(new JLabel(""));
        }
        
        pnlDays.revalidate();
        pnlDays.repaint();
    }
}
