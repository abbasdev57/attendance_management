import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class AttendanceSheetDialog extends JDialog {
    private final JDateChooser fromDateChooser;
    private final JDateChooser toDateChooser;
    private final JTable table;
    private final DefaultTableModel model;

    public AttendanceSheetDialog(JFrame parent) {
        super(parent, "Attendance Sheet", true);
        setLayout(new BorderLayout(10, 10));
        setSize(700, 500);
        setLocationRelativeTo(parent);

        // Top: date range + refresh button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.add(new JLabel("From:"));
        fromDateChooser = new JDateChooser();
        fromDateChooser.setPreferredSize(new Dimension(120, 25));
        topPanel.add(fromDateChooser);
        topPanel.add(new JLabel("To:"));
        toDateChooser = new JDateChooser();
        toDateChooser.setPreferredSize(new Dimension(120, 25));
        topPanel.add(toDateChooser);

        JButton refreshBtn = new JButton("Refresh");
        topPanel.add(refreshBtn);
        JButton closeBtn = new JButton("Close");
        topPanel.add(closeBtn);
        add(topPanel, BorderLayout.NORTH);

        // Table (will be rebuilt)
        model = new DefaultTableModel();
        table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        // Load initial (last 30 days)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        fromDateChooser.setDate(cal.getTime());
        toDateChooser.setDate(new Date());

        refreshSheet();

        refreshBtn.addActionListener(e -> refreshSheet());
        closeBtn.addActionListener(e -> dispose());
    }

    private void refreshSheet() {
        Date from = fromDateChooser.getDate();
        Date to = toDateChooser.getDate();
        if (from == null || to == null) {
            JOptionPane.showMessageDialog(this, "Please select both dates.", "Missing Range", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (from.after(to)) {
            JOptionPane.showMessageDialog(this, "From date must be before To date.", "Invalid Range", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get matrix: studentId -> (date -> status)
        Map<String, Map<String, String>> matrix = DatabaseHelper.getAttendanceMatrix(from, to);

        // Get all students (including those with no attendance in range)
        List<DatabaseHelper.Student> allStudents = DatabaseHelper.getAllStudents();

        // Collect all distinct dates that appear in the matrix (or we could generate all dates in range,
        // but we only show dates that have at least one attendance record)
        Set<String> dateSet = new TreeSet<>();
        for (Map<String, String> perStudent : matrix.values()) {
            dateSet.addAll(perStudent.keySet());
        }
        // If no dates, show empty message
        if (dateSet.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No attendance records in the selected date range.", "Info", JOptionPane.INFORMATION_MESSAGE);
            model.setRowCount(0);
            model.setColumnCount(0);
            return;
        }

        // Create column list: first column "Student", then each date
        java.util.List<String> columns = new ArrayList<>();
        columns.add("Student");
        columns.addAll(dateSet);

        // Build rows: for each student, create row with Student ID (or ID - Name) and status for each date
        model.setRowCount(0);
        model.setColumnCount(0);
        for (String col : columns) {
            model.addColumn(col);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (DatabaseHelper.Student student : allStudents) {
            Object[] row = new Object[columns.size()];
            row[0] = student.studentId + " - " + student.name;
            Map<String, String> studentData = matrix.getOrDefault(student.studentId, new HashMap<>());
            for (int i = 1; i < columns.size(); i++) {
                String date = columns.get(i);
                String status = studentData.get(date);
                // Convert status: "Present" -> "P", "Absent" -> "A", else empty
                if ("Present".equalsIgnoreCase(status)) {
                    row[i] = "P";
                } else if ("Absent".equalsIgnoreCase(status)) {
                    row[i] = "A";
                } else {
                    row[i] = "";
                }
            }
            model.addRow(row);
        }

        // Optionally, sort rows by Student ID (already sorted from DB)
        // We can also add a total row? Not necessary.
    }
}