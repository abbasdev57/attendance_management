import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class AttendanceGUI extends JFrame {
    // UI components (made package-private so controller can access them if needed)
    JDateChooser dateChooser;
    JComboBox<String> cmbStudent;
    DefaultComboBoxModel<String> studentModel;
    Map<String, String> studentMap = new HashMap<>();
    JComboBox<String> cmbStatus;
    JTable table;
    DefaultTableModel tableModel;
    TableRowSorter<DefaultTableModel> sorter;
    JTextField searchStudentField;
    JDateChooser fromDateChooser;
    JDateChooser toDateChooser;

    private AttendanceController controller;

    public AttendanceGUI() {
        controller = new AttendanceController(this);
        initUI();
        controller.initialize(); // loads data, migrates CSV, etc.
    }

    private void initUI() {
        setTitle("Advanced Attendance System");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // ---- Input Panel ----
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        inputPanel.add(new JLabel("Date:"));
        dateChooser = new JDateChooser();
        dateChooser.setDate(new java.util.Date());
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
        tableModel = new DefaultTableModel(new String[]{"ID", "Date", "Student ID", "Name", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        // Center align cells and headers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

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

        // ---- Event Listeners (delegate to controller) ----
        btnAdd.addActionListener(e -> controller.markAttendance());
        btnEdit.addActionListener(e -> controller.editAttendance());
        btnDelete.addActionListener(e -> controller.deleteAttendance());
        btnFilter.addActionListener(e -> controller.applyFilter());
        btnResetFilter.addActionListener(e -> controller.resetFilter());
        btnExport.addActionListener(e -> controller.exportCSV());
        btnClearAll.addActionListener(e -> controller.clearAll());
        btnAddStudent.addActionListener(e -> controller.addStudent());
        btnManageStudents.addActionListener(e -> new ManageStudentsDialog(this, controller::refreshAll));
        btnSheet.addActionListener(e -> new AttendanceSheetDialog(this));
        searchStudentField.addActionListener(e -> controller.applyFilter());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AttendanceGUI().setVisible(true));
    }
}