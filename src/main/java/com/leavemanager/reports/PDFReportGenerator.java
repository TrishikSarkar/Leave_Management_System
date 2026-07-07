package com.leavemanager.reports;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.leavemanager.dao.EmployeeDAO;
import com.leavemanager.dao.LeaveBalanceDAO;
import com.leavemanager.dao.LeaveRequestDAO;
import com.leavemanager.models.Employee;
import com.leavemanager.models.LeaveBalance;
import com.leavemanager.models.LeaveRequest;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PDFReportGenerator {

    public static void generateReport(Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save PDF Report");
        fileChooser.setSelectedFile(new File("Leave_Report.pdf"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Documents (*.pdf)", "pdf"));

        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        String filePath = fileToSave.getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".pdf")) {
            filePath += ".pdf";
        }

        try {
            writeReport(filePath);
            JOptionPane.showMessageDialog(parent, "PDF report exported successfully to:\n" + filePath, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.err.println("Error generating PDF Report");
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Failed to generate PDF Report:\n" + e.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Programmatically writes the leave report PDF file to the specified path without popping up a JFileChooser.
     * Useful for testing and CLI operations.
     */
    public static void writeReport(String filePath) throws Exception {
        Document document = new Document(PageSize.A4);
        FileOutputStream fos = new FileOutputStream(filePath);
        try {
            PdfWriter.getInstance(document, fos);
            document.open();

            // Load DAOs
            EmployeeDAO employeeDAO = new EmployeeDAO();
            LeaveBalanceDAO balanceDAO = new LeaveBalanceDAO();
            LeaveRequestDAO requestDAO = new LeaveRequestDAO();

            // Custom Fonts
            Font mainTitleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 80, 150));
            Font subtitleFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.GRAY);
            Font sectionTitleFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(50, 50, 50));
            Font tableHeaderFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font tableBodyFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);

            // Document Header
            Paragraph mainTitle = new Paragraph("Hindalco Industries Ltd.", mainTitleFont);
            mainTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(mainTitle);

            Paragraph subTitle = new Paragraph("IT Department Internship - Employee Leave Management System\n" +
                    "Centralized System Leave Report\n" +
                    "Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n\n", subtitleFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subTitle);

            // Section 1: Employee Directory & Leave Balances
            Paragraph sec1Title = new Paragraph("1. Employee Records and Leave Balances\n\n", sectionTitleFont);
            document.add(sec1Title);

            PdfPTable empTable = new PdfPTable(7);
            empTable.setWidthPercentage(100);
            empTable.setWidths(new float[]{1.0f, 2.5f, 2.0f, 2.0f, 1.2f, 1.2f, 1.2f});

            // Set Table Headers
            String[] empHeaders = {"ID", "Name", "Department", "Designation", "Casual", "Sick", "Earned"};
            for (String header : empHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
                cell.setBackgroundColor(new Color(30, 80, 150));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                empTable.addCell(cell);
            }

            // Fill Table Data
            List<Employee> employees = employeeDAO.getAllEmployees();
            for (Employee emp : employees) {
                LeaveBalance balance = balanceDAO.getLeaveBalance(emp.getId());
                int casual = balance != null ? balance.getCasualLeave() : 0;
                int sick = balance != null ? balance.getSickLeave() : 0;
                int earned = balance != null ? balance.getEarnedLeave() : 0;

                empTable.addCell(createCell(String.valueOf(emp.getId()), tableBodyFont, Element.ALIGN_CENTER));
                empTable.addCell(createCell(emp.getName(), tableBodyFont, Element.ALIGN_LEFT));
                empTable.addCell(createCell(emp.getDepartment(), tableBodyFont, Element.ALIGN_LEFT));
                empTable.addCell(createCell(emp.getDesignation(), tableBodyFont, Element.ALIGN_LEFT));
                empTable.addCell(createCell(String.valueOf(casual), tableBodyFont, Element.ALIGN_CENTER));
                empTable.addCell(createCell(String.valueOf(sick), tableBodyFont, Element.ALIGN_CENTER));
                empTable.addCell(createCell(String.valueOf(earned), tableBodyFont, Element.ALIGN_CENTER));
            }
            document.add(empTable);
            document.add(new Paragraph("\n\n"));

            // Section 2: Leave Requests History
            Paragraph sec2Title = new Paragraph("2. Leave Application History Log\n\n", sectionTitleFont);
            document.add(sec2Title);

            PdfPTable reqTable = new PdfPTable(6);
            reqTable.setWidthPercentage(100);
            reqTable.setWidths(new float[]{1.2f, 2.5f, 2.0f, 1.8f, 1.8f, 1.5f});

            String[] reqHeaders = {"Req ID", "Employee", "Leave Type", "Start Date", "End Date", "Status"};
            for (String header : reqHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
                cell.setBackgroundColor(new Color(30, 80, 150));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                reqTable.addCell(cell);
            }

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            List<LeaveRequest> requests = requestDAO.getAllLeaveRequests();
            for (LeaveRequest req : requests) {
                reqTable.addCell(createCell(String.valueOf(req.getId()), tableBodyFont, Element.ALIGN_CENTER));
                reqTable.addCell(createCell(req.getEmployeeName(), tableBodyFont, Element.ALIGN_LEFT));
                reqTable.addCell(createCell(req.getLeaveType(), tableBodyFont, Element.ALIGN_LEFT));
                reqTable.addCell(createCell(df.format(req.getStartDate()), tableBodyFont, Element.ALIGN_CENTER));
                reqTable.addCell(createCell(df.format(req.getEndDate()), tableBodyFont, Element.ALIGN_CENTER));
                
                PdfPCell statusCell = createCell(req.getStatus(), tableBodyFont, Element.ALIGN_CENTER);
                if ("Approved".equalsIgnoreCase(req.getStatus())) {
                    statusCell.setBackgroundColor(new Color(230, 245, 230)); // light green
                } else if ("Rejected".equalsIgnoreCase(req.getStatus())) {
                    statusCell.setBackgroundColor(new Color(255, 230, 230)); // light red
                } else {
                    statusCell.setBackgroundColor(new Color(255, 250, 230)); // light yellow
                }
                reqTable.addCell(statusCell);
            }
            document.add(reqTable);
            document.close();
        } finally {
            fos.close();
        }
    }

    private static PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}
