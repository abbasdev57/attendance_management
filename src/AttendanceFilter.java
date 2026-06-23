import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AttendanceFilter {

    public static class DateRangeFilter extends RowFilter<Object, Object> {
        private final Date from;
        private final Date to;
        private final SimpleDateFormat sdf;

        public DateRangeFilter(Date from, Date to, String pattern) {
            this.from = from;
            this.to = to;
            this.sdf = new SimpleDateFormat(pattern);
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
}