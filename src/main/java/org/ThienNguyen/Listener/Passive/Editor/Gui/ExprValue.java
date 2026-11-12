package org.ThienNguyen.Listener.Passive.Editor.Gui;

/**
 * Chuyển chuỗi nhập cho field EXPRESSION: chỉ gồm số thì lưu thành số (YAML gọn: {@code amount: 5}),
 * còn lại (biểu thức, %placeholder%, khoảng "1-3", ...) giữ nguyên chuỗi. Plugin đọc bằng getString nên cả hai đều hợp lệ.
 */
final class ExprValue {

    private ExprValue() {}

    static Object of(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.matches("-?\\d{1,9}")) return Integer.valueOf(s);
        if (s.matches("-?\\d{1,7}\\.\\d{1,6}")) {
            double d = Double.parseDouble(s);
            double a = Math.abs(d);
            // Double.toString dùng ký hiệu khoa học khi < 1e-3 -> giữ chuỗi để plugin đọc đúng
            if (a == 0 || a >= 1e-3) return Double.valueOf(d);
        }
        return s;
    }
}
