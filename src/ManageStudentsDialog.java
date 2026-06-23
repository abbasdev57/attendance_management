import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class ManageStudentsDialog extends JDialog {
    private final JTable table;
    private final DefaultTableModel model;
    private final Runnable onRefresh; // callback to refresh main dropdown

    public ManageStudentsDialog(JFrame parent, Runnable refreshCallback) {
        super(parent, "Manage Students", true);
        this.onRefresh = refreshCallback;

        setLayout(new BorderLayout(10, 10));
        setSize(500, 400);
        setLocationRelativeTo(parent);

        // Table: columns "ID", "Student ID", "Name" (hide ID)
        model = new DefaultTableModel(new String[]{"ID", "Student ID", "Name"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);
        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        // Buttons panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton closeBtn = new JButton("Close");

        btnPanel.add(addBtn);
        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Load data
        loadStudents();

        // Listeners
        addBtn.addActionListener(e -> addStudent());
        editBtn.addActionListener(e -> editStudent());
        deleteBtn.addActionListener(e -> deleteStudent());
        closeBtn.addActionListener(e -> dispose());

        setVisible(true);
    }

    private void loadStudents() {
        model.setRowCount(0);
        List<DatabaseHelper.Student> students = DatabaseHelper.getAllStudents();
        for (DatabaseHelper.Student s : students) {
            model.addRow(new Object[]{s.id, s.studentId, s.name});
        }
    }

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
            JOptionPane.showMessageDialog(this, "Both fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DatabaseHelper.insertStudent(sid, name);
            loadStudents();
            if (onRefresh != null) onRefresh.run();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                JOptionPane.showMessageDialog(this, "Student ID already exists.", "Duplicate", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "DB error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editStudent() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a student to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String oldSid = (String) model.getValueAt(row, 1);
        String oldName = (String) model.getValueAt(row, 2);

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField idField = new JTextField(oldSid);
        JTextField nameField = new JTextField(oldName);
        panel.add(new JLabel("Student ID:"));
        panel.add(idField);
        panel.add(new JLabel("Full Name:"));
        panel.add(nameField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Edit Student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newSid = idField.getText().trim();
        String newName = nameField.getText().trim();
        if (newSid.isEmpty() || newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DatabaseHelper.updateStudent(id, newSid, newName);
            loadStudents();
            if (onRefresh != null) onRefresh.run();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                JOptionPane.showMessageDialog(this, "New Student ID already exists.", "Duplicate", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "DB error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteStudent() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a student to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String sid = (String) model.getValueAt(row, 1);
        String name = (String) model.getValueAt(row, 2);

        int count = DatabaseHelper.countAttendanceForStudent(sid);
        String msg = "Delete student '" + sid + " - " + name + "'?";
        if (count > 0) {
            msg += "\nThis student has " + count + " attendance record(s). Deleting will also remove all their attendance records.";
        }
        int confirm = JOptionPane.showConfirmDialog(this, msg, "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            // Delete attendance first (cascade)
            // We'll delete all attendance for this student
            String delAtt = "DELETE FROM attendance WHERE student_id = ?";
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(delAtt)) {
                pstmt.setString(1, sid);
                pstmt.executeUpdate();
            }
            // Then delete student
            DatabaseHelper.deleteStudent(id);
            loadStudents();
            if (onRefresh != null) onRefresh.run();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}