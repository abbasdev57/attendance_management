import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class AttendanceGUI extends JFrame {
    private static final String CSV_FILE = "attendance_records.csv";
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private JDateChooser dateChooser;
    private JComboBox<String> cmbStudent;
    private DefaultComboBoxModel<String> studentModel;
    private final Map<String, String> studentMap = new HashMap<>(); // student_id -> name
    private JComboBox<String> cmbStatus;
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchStudentField;
    private JDateChooser fromDateChooser;
    private JDateChooser toDateChooser;

    public AttendanceGUI() {
        setTitle("Advanced Attendance System");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // ---- Input Panel ----
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        inputPanel.add(new JLabel("Date:"));
        dateChooser = new JDateChooser();
        dateChooser.setDate(new Date());
        dateChooser.setPreferredSize(new Dimension(120, 25));
        inputPanel.add(dateChooser);

        inputPanel.add(new JLabel("Student:"));
        studentModel = new DefaultComboBoxModel<>();
        cmbStudent = new JComboBox<>(studentModel);
        cmbStudent.setPreferredSize(new Dimension(150, 25));
        cmbStudent.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                String display = (String) value;
                if (display != null && studentMap.containsKey(display)) {
                    display = display + " - " + studentMap.get(display);
                }
                return super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
            }
        });
        inputPanel.add(cmbStudent);

        JButton btnAddStudent = new JButton("Add Student");
        inputPanel.add(btnAddStudent);

        JButton btnManageStudents = new JButton("Manage Students");
        inputPanel.add(btnManageStudents);

        inputPanel.add(new JLabel("Status:"));
        cmbStatus = new JComboBox<>(new String[]{"Present", "Absent"});
        inputPanel.add(cmbStatus);

        JButton btnAdd = new JButton("Mark Attendance");
        inputPanel.add(btnAdd);

        JButton btnEdit = new JButton("Edit");
        inputPanel.add(btnEdit);

        JButton btnDelete = new JButton("Delete");
        inputPanel.add(btnDelete);

        JButton btnSheet = new JButton("Attendance Sheet");
        inputPanel.add(btnSheet);

        add(inputPanel, BorderLayout.NORTH);

        // ---- Table ----
        // Columns: hidden ID, Date, Student ID, Name, Status
        tableModel = new DefaultTableModel(new String[]{"ID", "Date", "Student ID", "Name", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // ---- Bottom: Filter & Export / Clear ----
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.add(new JLabel("Search Student ID:"));
        searchStudentField = new JTextField(10);
        filterPanel.add(searchStudentField);

        filterPanel.add(new JLabel("From:"));
        fromDateChooser = new JDateChooser();
        fromDateChooser.setPreferredSize(new Dimension(100, 25));
        filterPanel.add(fromDateChooser);

        filterPanel.add(new JLabel("To:"));
        toDateChooser = new JDateChooser();
        toDateChooser.setPreferredSize(new Dimension(100, 25));
        filterPanel.add(toDateChooser);

        JButton btnFilter = new JButton("Apply Filter");
        filterPanel.add(btnFilter);

        JButton btnResetFilter = new JButton("Reset");
        filterPanel.add(btnResetFilter);

        bottomPanel.add(filterPanel, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnExport = new JButton("Export CSV");
        actionPanel.add(btnExport);
        JButton btnClearAll = new JButton("Clear All");
        actionPanel.add(btnClearAll);
        bottomPanel.add(actionPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // ---- Database init & load ----
        DatabaseHelper.initDatabase();
        migrateCSVtoDB(); // one-time migration
        refreshStudentDropdown();
        loadAttendance();

        // ---- Event Listeners ----
        btnAdd.addActionListener(e -> markAttendance());
        btnEdit.addActionListener(e -> editAttendance());
        btnDelete.addActionListener(e -> deleteAttendance());
        btnFilter.addActionListener(e -> applyFilter());
        btnResetFilter.addActionListener(e -> resetFilter());
        btnExport.addActionListener(e -> exportCSV());
        btnClearAll.addActionListener(e -> clearAll());
        btnAddStudent.addActionListener(e -> addStudent());
        btnManageStudents.addActionListener(e -> new ManageStudentsDialog(this, this::refreshAll));
        btnSheet.addActionListener(e -> new AttendanceSheetDialog(this));
        searchStudentField.addActionListener(e -> applyFilter());
    }

    // ---------- Refreshes ----------
    private void refreshAll() {
        refreshStudentDropdown();
        loadAttendance();
        applyFilter(); // keep filter if any
    }

    private void refreshStudentDropdown() {
        studentModel.removeAllElements();
        studentMap.clear();
        List<DatabaseHelper.Student> students = DatabaseHelper.getAllStudents();
        for (DatabaseHelper.Student s : students) {
            studentMap.put(s.studentId, s.name);
            studentModel.addElement(s.studentId);
        }
        if (studentModel.getSize() == 0) {
            studentModel.addElement("[No students]");
            cmbStudent.setEnabled(false);
        } else {
            cmbStudent.setEnabled(true);
        }
    }

    private void loadAttendance() {
        tableModel.setRowCount(0);
        List<DatabaseHelper.AttendanceRecord> records = DatabaseHelper.getAllAttendanceWithNames();
        for (DatabaseHelper.AttendanceRecord r : records) {
            Object[] row = {
                    r.id,
                    r.date,
                    r.studentId,
                    r.studentName != null ? r.studentName : "[Deleted]",
                    r.status
            };
            tableModel.addRow(row);
        }
    }

    // ---------- Attendance operations ----------
    private void markAttendance() {
        if (!cmbStudent.isEnabled() || cmbStudent.getSelectedItem() == null ||
                "[No students]".equals(cmbStudent.getSelectedItem())) {
            JOptionPane.showMessageDialog(this, "Please add a student first.",
                    "No Student", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String studentId = (String) cmbStudent.getSelectedItem();
        String date = new SimpleDateFormat(DATE_PATTERN).format(dateChooser.getDate());
        String status = (String) cmbStatus.getSelectedItem();

        try {
            DatabaseHelper.insertAttendance(date, studentId, status);
            loadAttendance();
            resetFilter();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error marking attendance: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editAttendance() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a record to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String oldDate = (String) tableModel.getValueAt(modelRow, 1);
        String oldStudent = (String) tableModel.getValueAt(modelRow, 2);
        String oldStatus = (String) tableModel.getValueAt(modelRow, 4);

        // Build edit dialog with student dropdown (same as before)
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        JDateChooser dateEdit = new JDateChooser();
        try {
            dateEdit.setDate(new SimpleDateFormat(DATE_PATTERN).parse(oldDate));
        } catch (Exception e) {
            dateEdit.setDate(new Date());
        }
        panel.add(new JLabel("Date:"));
        panel.add(dateEdit);

        // Student dropdown
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

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Edit Record", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newDate = new SimpleDateFormat(DATE_PATTERN).format(dateEdit.getDate());
        String newStudent = (String) cmbStudentEdit.getSelectedItem();
        String newStatus = (String) statusEdit.getSelectedItem();
        if (newStudent == null || newStudent.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invalid student selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DatabaseHelper.updateAttendance(id, newDate, newStudent, newStatus);
            loadAttendance();
            applyFilter();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Update failed: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteAttendance() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a record to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int id = (int) tableModel.getValueAt(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this attendance record?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            DatabaseHelper.deleteAttendance(id);
            loadAttendance();
            applyFilter();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAll() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete ALL attendance records?", "Clear All", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            DatabaseHelper.clearAllAttendance();
            loadAttendance();
            resetFilter();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Clear failed: " + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- Filter ----------
    private void applyFilter() {
        String studentId = searchStudentField.getText().trim();
        Date fromDate = fromDateChooser.getDate();
        Date toDate = toDateChooser.getDate();

        java.util.List<RowFilter<Object, Object>> filters = new ArrayList<>();
        if (!studentId.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + studentId, 2)); // column 2 = Student ID
        }
        if (fromDate != null || toDate != null) {
            filters.add(new DateRangeFilter(fromDate, toDate));
        }
        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private void resetFilter() {
        searchStudentField.setText("");
        fromDateChooser.setDate(null);
        toDateChooser.setDate(null);
        sorter.setRowFilter(null);
    }

    private static class DateRangeFilter extends RowFilter<Object, Object> {
        private final Date from, to;
        private final SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);

        DateRangeFilter(Date from, Date to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean include(Entry<?, ?> entry) {
            String dateStr = (String) entry.getValue(1); // column 1 = Date
            try {
                Date d = sdf.parse(dateStr);
                if (from != null && d.before(from)) return false;
                if (to != null && d.after(to)) return false;
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    // ---------- Export CSV ----------
    private void exportCSV() {
        int rowCount = table.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "No data to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("attendance_export.csv"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Date,Student ID,Name,Status");
            for (int i = 0; i < rowCount; i++) {
                int modelRow = table.convertRowIndexToModel(i);
                String date = (String) tableModel.getValueAt(modelRow, 1);
                String sid = (String) tableModel.getValueAt(modelRow, 2);
                String name = (String) tableModel.getValueAt(modelRow, 3);
                String status = (String) tableModel.getValueAt(modelRow, 4);
                writer.println(escapeCSV(date) + "," + escapeCSV(sid) + "," +
                        escapeCSV(name) + "," + escapeCSV(status));
            }
            JOptionPane.showMessageDialog(this, "Export completed: " + file.getAbsolutePath(),
                    "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(),
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

    // ---------- Add Student (shortcut) ----------
    private void addStudent() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(15);
        panel.add(new JLabel("Student ID:"));
        panel.add(idField);
        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Add Student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String sid = idField.getText().trim();
        String name = nameField.getText().trim();
        if (sid.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both fields required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DatabaseHelper.insertStudent(sid, name);
            refreshAll();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                JOptionPane.showMessageDialog(this, "Student ID already exists.", "Duplicate", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "DB error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ---------- CSV migration (one-time) ----------
    private void migrateCSVtoDB() {
        // Only if DB attendance table is empty and CSV file exists
        if (!new File(CSV_FILE).exists()) return;
        if (DatabaseHelper.getAllAttendanceWithNames().size() > 0) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE))) {
            String line;
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                // Simple CSV split (no quoted fields in old format)
                String[] parts = line.split(",");
                if (parts.length < 3) continue;
                String date = parts[0].trim();
                String sid = parts[1].trim();
                String status = parts[2].trim();
                // Validate date
                try {
                    sdf.parse(date);
                } catch (Exception e) {
                    continue;
                }
                // Insert student if not exists (with placeholder name)
                if (!DatabaseHelper.studentExists(sid)) {
                    DatabaseHelper.insertStudent(sid, "Migrated: " + sid);
                }
                DatabaseHelper.insertAttendance(date, sid, status);
            }
            JOptionPane.showMessageDialog(this, "Migrated old CSV data into database.",
                    "Migration", JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
        } catch (Exception e) {
            System.err.println("CSV migration error: " + e.getMessage());
        }
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AttendanceGUI().setVisible(true));
    }
}