# 📋 Attendance System

A feature‑rich Java Swing desktop application for managing student attendance. Built with SQLite for data persistence and a clean, modular architecture.

---

## 🚀 Features

- **Student Management** – Add, edit, or delete students. Deleting a student automatically removes all their attendance records (cascade).
- **Attendance Tracking** – Mark students as **Present** or **Absent** for any date. Multiple entries per student per day are allowed (no duplicate restriction).
- **Attractive Table View** – The main table displays **Date**, **Student ID**, **Student Name**, and **Status**, with all cells and headers **centered** for better readability.
- **Powerful Filtering** – Filter records by Student ID (partial match) and/or a date range.
- **Attendance Sheet (Matrix View)** – See a **pivot table** with students as rows and dates as columns, where each cell shows **P** (Present) or **A** (Absent). Date range selectable.
- **Export to CSV** – Export the **currently filtered** data to a properly quoted CSV file with headers.
- **Data Migration** – Automatically imports an existing `attendance_records.csv` (from older versions) into the SQLite database on first run.
- **Modular Code** – Separated into logical classes: `DatabaseHelper`, `ManageStudentsDialog`, `AttendanceSheetDialog`, and `AttendanceGUI`.

---

## 🛠️ Technologies Used

- **Java 8+** (Swing for GUI)
- **SQLite** (embedded database, `sqlite-jdbc` driver)
- **JCalendar** (`com.toedter:jcalendar`) – date picker widgets
- **Maven** (optional – dependencies below)

---

## License
This project is open‑source and free to use for educational or personal purposes.

## 📧 Feedback & Support
For issues or feature requests, please open an issue on the project repository (or contact the maintainer).

## ScreenShot
<img width="1136" height="650" alt="Screenshot (40)" src="https://github.com/user-attachments/assets/03870e2d-fb2e-45ce-a72b-baec6fb4e748" />
