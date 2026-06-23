import java.sql.*;
import java.util.*;
import java.util.Date;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:attendance.db";

    // ---------- Connection ----------
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // ---------- Initialisation ----------
    public static void initDatabase() {
        String sqlStudents = "CREATE TABLE IF NOT EXISTS students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "student_id TEXT UNIQUE NOT NULL," +
                "name TEXT NOT NULL)";
        String sqlAttendance = "CREATE TABLE IF NOT EXISTS attendance (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT NOT NULL," +
                "student_id TEXT NOT NULL," +
                "status TEXT NOT NULL)";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlStudents);
            stmt.execute(sqlAttendance);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Student operations ----------
    public static List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT id, student_id, name FROM students ORDER BY student_id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Student(rs.getInt("id"),
                        rs.getString("student_id"),
                        rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean studentExists(String studentId) {
        String sql = "SELECT 1 FROM students WHERE student_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void insertStudent(String studentId, String name) throws SQLException {
        String sql = "INSERT INTO students(student_id, name) VALUES(?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        }
    }

    public static void updateStudent(int id, String newStudentId, String newName) throws SQLException {
        String sql = "UPDATE students SET student_id = ?, name = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStudentId);
            pstmt.setString(2, newName);
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
        }
    }

    public static void deleteStudent(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public static int countAttendanceForStudent(String studentId) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE student_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        }
    }

    // ---------- Attendance operations ----------
    public static List<AttendanceRecord> getAllAttendanceWithNames() {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT a.id, a.date, a.student_id, s.name AS student_name, a.status " +
                "FROM attendance a LEFT JOIN students s ON a.student_id = s.student_id " +
                "ORDER BY a.date DESC, a.id DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new AttendanceRecord(
                        rs.getInt("id"),
                        rs.getString("date"),
                        rs.getString("student_id"),
                        rs.getString("student_name"), // may be null
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void insertAttendance(String date, String studentId, String status) throws SQLException {
        String sql = "INSERT INTO attendance(date, student_id, status) VALUES(?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            pstmt.setString(2, studentId);
            pstmt.setString(3, status);
            pstmt.executeUpdate();
        }
    }

    public static void updateAttendance(int id, String date, String studentId, String status) throws SQLException {
        String sql = "UPDATE attendance SET date=?, student_id=?, status=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            pstmt.setString(2, studentId);
            pstmt.setString(3, status);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        }
    }

    public static void deleteAttendance(int id) throws SQLException {
        String sql = "DELETE FROM attendance WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public static void clearAllAttendance() throws SQLException {
        String sql = "DELETE FROM attendance";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    // ---------- Attendance Sheet (pivot) ----------
    public static Map<String, Map<String, String>> getAttendanceMatrix(Date from, Date to) {
        // Returns: Map<studentId, Map<dateString, status>>
        // Dates are expected as yyyy-MM-dd strings.
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String fromStr = (from != null) ? sdf.format(from) : "1970-01-01";
        String toStr = (to != null) ? sdf.format(to) : "2099-12-31";

        Map<String, Map<String, String>> matrix = new LinkedHashMap<>();
        String sql = "SELECT student_id, date, status FROM attendance " +
                "WHERE date BETWEEN ? AND ? ORDER BY date, student_id";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fromStr);
            pstmt.setString(2, toStr);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String date = rs.getString("date");
                String status = rs.getString("status");
                matrix.computeIfAbsent(studentId, k -> new LinkedHashMap<>())
                        .put(date, status);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return matrix;
    }

    // ---------- Data classes ----------
    public static class Student {
        public final int id;
        public final String studentId;
        public final String name;

        public Student(int id, String studentId, String name) {
            this.id = id;
            this.studentId = studentId;
            this.name = name;
        }
    }

    public static class AttendanceRecord {
        public final int id;
        public final String date;
        public final String studentId;
        public final String studentName;
        public final String status;

        public AttendanceRecord(int id, String date, String studentId,
                                String studentName, String status) {
            this.id = id;
            this.date = date;
            this.studentId = studentId;
            this.studentName = studentName;
            this.status = status;
        }
    }

    // ---------- CSV migration (used once) ----------
    public static void migrateCSVtoDB(String csvFile) {
        // Implementation from previous code (kept for completeness)
        // ... (we'll include it in the main class for clarity)
    }
}