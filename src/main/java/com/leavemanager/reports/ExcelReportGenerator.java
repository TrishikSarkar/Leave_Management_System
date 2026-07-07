package com.leavemanager.reports;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.leavemanager.dao.EmployeeDAO;
import com.leavemanager.dao.LeaveBalanceDAO;
import com.leavemanager.dao.LeaveRequestDAO;
import com.leavemanager.models.Employee;
import com.leavemanager.models.LeaveBalance;
import com.leavemanager.models.LeaveRequest;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ExcelReportGenerator {

    public static void generateReport(Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Excel Report");
        fileChooser.setSelectedFile(new File("Leave_Report.xlsx"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Spreadsheets (*.xlsx)", "xlsx"));

        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        String filePath = fileToSave.getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".xlsx")) {
            filePath += ".xlsx";
        }

        try {
            writeReport(filePath);
            JOptionPane.showMessageDialog(parent, "Excel report exported successfully to:\n" + filePath, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.err.println("Error generating Excel Report");
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Failed to generate Excel Report:\n" + e.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Programmatically writes the leave report Excel workbook to the specified path without popping up a JFileChooser.
     * Useful for testing and CLI operations.
     */
    public static void writeReport(String filePath) throws Exception {
        // Initialize DAOs
        EmployeeDAO employeeDAO = new EmployeeDAO();
        LeaveBalanceDAO balanceDAO = new LeaveBalanceDAO();
        LeaveRequestDAO requestDAO = new LeaveRequestDAO();

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath)) {

            // Create Cell Styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));

            // --- SHEET 1: EMPLOYEE LEAVE BALANCES ---
            Sheet sheet1 = workbook.createSheet("Employee Balances");
            String[] headers1 = {"Employee ID", "Username", "Name", "Email", "Department", "Designation", "Casual Leave", "Sick Leave", "Earned Leave"};

            // Header Row
            Row headerRow1 = sheet1.createRow(0);
            for (int i = 0; i < headers1.length; i++) {
                Cell cell = headerRow1.createCell(i);
                cell.setCellValue(headers1[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            List<Employee> employees = employeeDAO.getAllEmployees();
            int rowIdx1 = 1;
            for (Employee emp : employees) {
                Row row = sheet1.createRow(rowIdx1++);
                row.createCell(0).setCellValue(emp.getId());
                row.createCell(1).setCellValue(emp.getUsername());
                row.createCell(2).setCellValue(emp.getName());
                row.createCell(3).setCellValue(emp.getEmail());
                row.createCell(4).setCellValue(emp.getDepartment());
                row.createCell(5).setCellValue(emp.getDesignation());

                LeaveBalance balance = balanceDAO.getLeaveBalance(emp.getId());
                int casual = balance != null ? balance.getCasualLeave() : 0;
                int sick = balance != null ? balance.getSickLeave() : 0;
                int earned = balance != null ? balance.getEarnedLeave() : 0;

                row.createCell(6).setCellValue(casual);
                row.createCell(7).setCellValue(sick);
                row.createCell(8).setCellValue(earned);
            }

            // Auto-size columns for sheet 1
            for (int i = 0; i < headers1.length; i++) {
                sheet1.autoSizeColumn(i);
            }

            // --- SHEET 2: LEAVE REQUESTS HISTORY ---
            Sheet sheet2 = workbook.createSheet("Leave Requests Log");
            String[] headers2 = {"Request ID", "Employee ID", "Employee Name", "Leave Type", "Start Date", "End Date", "Duration (Days)", "Status", "Reason", "Admin Comments"};

            // Header Row
            Row headerRow2 = sheet2.createRow(0);
            for (int i = 0; i < headers2.length; i++) {
                Cell cell = headerRow2.createCell(i);
                cell.setCellValue(headers2[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            List<LeaveRequest> requests = requestDAO.getAllLeaveRequests();
            int rowIdx2 = 1;
            for (LeaveRequest req : requests) {
                Row row = sheet2.createRow(rowIdx2++);
                
                long diffMs = req.getEndDate().getTime() - req.getStartDate().getTime();
                int days = (int) (diffMs / (1000 * 60 * 60 * 24)) + 1;

                row.createCell(0).setCellValue(req.getId());
                row.createCell(1).setCellValue(req.getEmployeeId());
                row.createCell(2).setCellValue(req.getEmployeeName());
                row.createCell(3).setCellValue(req.getLeaveType());

                Cell startCell = row.createCell(4);
                startCell.setCellValue(req.getStartDate());
                startCell.setCellStyle(dateStyle);

                Cell endCell = row.createCell(5);
                endCell.setCellValue(req.getEndDate());
                endCell.setCellStyle(dateStyle);

                row.createCell(6).setCellValue(days);
                row.createCell(7).setCellValue(req.getStatus());
                row.createCell(8).setCellValue(req.getReason());
                row.createCell(9).setCellValue(req.getComments() != null ? req.getComments() : "");
            }

            // Auto-size columns for sheet 2
            for (int i = 0; i < headers2.length; i++) {
                sheet2.autoSizeColumn(i);
            }

            // Write workbook to file
            workbook.write(fos);
        }
    }
}
