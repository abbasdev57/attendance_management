import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class AttendanceController {
    private final AttendanceGUI gui;
    private static final String CSV_FILE = "attendance_records.csv";
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    public AttendanceController(AttendanceGUI gui) {
        this.gui = gui;
    }

    // ---------- Initialisation ----------
    public void initialize() {
        DatabaseHelper.initDatabase();
        migrateCSVtoDB();
        refreshStudentDropdown();
        loadAttendance();
    }

    // ---------- Refreshes ----------
    public void refreshAll() {
        refreshStudentDropdown();
        loadAttendance();
        applyFilter(); // reapply current filter
    }

    private void refreshStudentDropdown() {
        gui.studentModel.removeAllElements();
        gui.studentMap.clear();
        List<DatabaseHelper.Student> students = DatabaseHelper.getAllStudents();
        for (DatabaseHelper.Student s : students) {
            gui.studentMap.put(s.studentId, s.name);
            gui.studentModel.addElement(s.studentId);
        }
        if (gui.studentModel.getSize() == 0) {
            gui.studentModel.addElement("[No students]");
            gui.cmbStudent.setEnabled(false);
        } else {
            gui.cmbStudent.setEnabled(true);
        }
    }

    private void loadAttendance() {
        gui.tableModel.setRowCount(0);
        List<DatabaseHelper.AttendanceRecord> records = DatabaseHelper.getAllAttendanceWithNames();
        for (DatabaseHelper.AttendanceRecord r : records) {
            Object[] row = {
                    r.id,
                    r.date,
                    r.studentId,
                    r.studentName != null ? r.studentName : "[Deleted]",
                    r.status
            };
            gui.tableModel.addRow(row);
        }
    }

    // ---------- Attendance Operations ----------
    public void markAttendance() {
        if (!gui.cmbStudent.isEnabled() || gui.cmbStudent.getSelectedItem() == null ||
                "[No students]".equals(gui.cmbStudent.getSelectedItem())) {
            JOptionPane.showMessageDialog(gui, "Please add a student first.",
                    "No Student", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String studentId = (String) gui.cmbStudent.getSelectedItem();
        String date = new SimpleDateFormat(DATE_PATTERN).format(gui.dateChooser.getDate());
        String status = (String) gui.cmbStatus.getSelectedItem();

        try {
            DatabaseHelper.insertAttendance(date, studentId, status);
            loadAttendance();
            resetFilter();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(gui, "Error marking attendance: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void editAttendance() {
        int row = gui.table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(gui, "Select a record to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = gui.table.convertRowIndexToModel(row);
        int id = (int) gui.tableModel.getValueAt(modelRow, 0);
        String oldDate = (String) gui.tableModel.getValueAt(modelRow, 1);
        String oldStudent = (String) gui.tableModel.getValueAt(modelRow, 2);
        String oldStatus = (String) gui.tableModel.getValueAt(modelRow, 4);

        // Build edit dialog
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        com.toedter.calendar.JDateChooser dateEdit = new com.toedter.calendar.JDateChooser();
        try {
            dateEdit.setDate(new SimpleDateFormat(DATE_PATTERN).parse(oldDate));
        } catch (Exception e) {
            dateEdit.setDate(new Date());
        }
        panel.add(new JLabel("Date:"));
        panel.add(dateEdit);

        JComboBox<String> cmbStudentEdit = new JComboBox<>();
        DefaultComboBoxModel<String> modelEdit = new DefaultComboBoxModel<>();
        Map<String, String> mapEdit = new HashMap<>();
        List<DatabaseHelper.Student> students = DatabaseHelper.getAllStudents();
        for (DatabaseHelper.Student s : students) {
            mapEdit.put(s.studentId, s.name);
            modelEdit.addElement(s.studentId);
        }
        cmbStudentEdit.setModel(modelEdit);
        cmbStudentEdit.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                String display = (String) value;
                if (display != null && mapEdit.containsKey(display)) {
                    display = display + " - " + mapEdit.get(display);
                }
                return super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
            }
        });
        cmbStudentEdit.setSelectedItem(oldStudent);
        panel.add(new JLabel("Student:"));
        panel.add(cmbStudentEdit);

        JComboBox<String> statusEdit = new JComboBox<>(new String[]{"Present", "Absent"});
        statusEdit.setSelectedItem(oldStatus);
        panel.add(new JLabel("Status:"));
        panel.add(statusEdit);

        int result = JOptionPane.showConfirmDialog(gui, panel,
                "Edit Record", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newDate = new SimpleDateFormat(DATE_PATTERN).format(dateEdit.getDate());
        String newStudent = (String) cmbStudentEdit.getSelectedItem();
        String newStatus = (String) statusEdit.getSelectedItem();
        if (newStudent == null || newStudent.isEmpty()) {
            JOptionPane.showMessageDialog(gui, "Invalid student selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DatabaseHelper.updateAttendance(id, newDate, newStudent, newStatus);
            loadAttendance();
            applyFilter();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(gui, "Update failed: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteAttendance() {
        int row = gui.table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(gui, "Select a record to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = gui.table.convertRowIndexToModel(row);
        int id = (int) gui.tableModel.getValueAt(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(gui,
                "Delete this attendance record?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            DatabaseHelper.deleteAttendance(id);
            loadAttendance();
            applyFilter();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(gui, "Delete failed: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearAll() {
        int confirm = JOptionPane.showConfirmDialog(gui,
                "Delete ALL attendance records?", "Clear All", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            DatabaseHelper.clearAllAttendance();
            loadAttendance();
            resetFilter();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(gui, "Clear failed: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- Filter ----------
    public void applyFilter() {
        String studentId = gui.searchStudentField.getText().trim();
        java.util.Date fromDate = gui.fromDateChooser.getDate();
        java.util.Date toDate = gui.toDateChooser.getDate();

        java.util.List<RowFilter<Object, Object>> filters = new ArrayList<>();
        if (!studentId.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + studentId, 2));
        }
        if (fromDate != null || toDate != null) {
            filters.add(new AttendanceFilter.DateRangeFilter(fromDate, toDate, DATE_PATTERN));
        }
        if (filters.isEmpty()) {
            gui.sorter.setRowFilter(null);
        } else {
            gui.sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    public void resetFilter() {
        gui.searchStudentField.setText("");
        gui.fromDateChooser.setDate(null);
        gui.toDateChooser.setDate(null);
        gui.sorter.setRowFilter(null);
    }

    // ---------- Export CSV ----------
    public void exportCSV() {
        int rowCount = gui.table.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(gui, "No data to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("attendance_export.csv"));
        if (fileChooser.showSaveDialog(gui) != JFileChooser.APPROVE_OPTION) return;

        File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Date,Student ID,Name,Status");
            for (int i = 0; i < rowCount; i++) {
                int modelRow = gui.table.convertRowIndexToModel(i);
                String date = (String) gui.tableModel.getValueAt(modelRow, 1);
                String sid = (String) gui.tableModel.getValueAt(modelRow, 2);
                String name = (String) gui.tableModel.getValueAt(modelRow, 3);
                String status = (String) gui.tableModel.getValueAt(modelRow, 4);
                writer.println(escapeCSV(date) + "," + escapeCSV(sid) + "," +
                        escapeCSV(name) + "," + escapeCSV(status));
            }
            JOptionPane.showMessageDialog(gui, "Export completed: " + file.getAbsolutePath(),
                    "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(gui, "Export failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ---------- Add Student ----------
    public void addStudent() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(15);
        panel.add(new JLabel("Student ID:"));
        panel.add(idField);
        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);

        int result = JOptionPane.showConfirmDialog(gui, panel,
                "Add Student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String sid = idField.getText().trim();
        String name = nameField.getText().trim();
        if (sid.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(gui, "Both fields required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DatabaseHelper.insertStudent(sid, name);
            refreshAll();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                JOptionPane.showMessageDialog(gui, "Student ID already exists.", "Duplicate", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(gui, "DB error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ---------- CSV Migration ----------
    private void migrateCSVtoDB() {
        if (!new File(CSV_FILE).exists()) return;
        if (DatabaseHelper.getAllAttendanceWithNames().size() > 0) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE))) {
            String line;
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 3) continue;
                String date = parts[0].trim();
                String sid = parts[1].trim();
                String status = parts[2].trim();
                try {
                    sdf.parse(date);
                } catch (Exception e) {
                    continue;
                }
                if (!DatabaseHelper.studentExists(sid)) {
                    DatabaseHelper.insertStudent(sid, "Migrated: " + sid);
                }
                DatabaseHelper.insertAttendance(date, sid, status);
            }
            JOptionPane.showMessageDialog(gui, "Migrated old CSV data into database.",
                    "Migration", JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
        } catch (Exception e) {
            System.err.println("CSV migration error: " + e.getMessage());
        }
    }
}