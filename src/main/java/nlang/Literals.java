package nlang;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Literals {

    private Literals() {
    }

    public record Num(BigInteger n, BigInteger d) implements Comparable<Num> {

        public static Num of(long v) {
            return new Num(BigInteger.valueOf(v), BigInteger.ONE);
        }

        public static Num reduce(BigInteger n, BigInteger d) {
            if (d.signum() == 0) {
                throw new IllegalArgumentException("分母不能为零");
            }
            if (d.signum() < 0) {
                n = n.negate();
                d = d.negate();
            }
            if (n.signum() == 0) {
                return new Num(BigInteger.ZERO, BigInteger.ONE);
            }
            BigInteger g = n.gcd(d);
            return new Num(n.divide(g), d.divide(g));
        }

        public static Num parse(String s) {
            boolean neg = s.startsWith("-");
            String body = neg ? s.substring(1) : s;
            Num r;
            int slash = body.indexOf('/');
            int dot = body.indexOf('.');
            if (slash >= 0) {
                String a = body.substring(0, slash);
                String b = body.substring(slash + 1);
                if (!a.matches("[0-9]+") || !b.matches("[0-9]+")) {
                    throw new IllegalArgumentException("分数形状不合法");
                }
                if (!b.matches("0*[1-9][0-9]*")) {
                    throw new IllegalArgumentException("分母必须为正整数且不能为零");
                }
                r = reduce(new BigInteger(a), new BigInteger(b));
            } else if (dot >= 0) {
                String a = body.substring(0, dot);
                String f = body.substring(dot + 1);
                if (!a.matches("[0-9]+") || !f.matches("[0-9]+")) {
                    throw new IllegalArgumentException("小数形状不合法");
                }
                BigInteger den = BigInteger.TEN.pow(f.length());
                BigInteger num = new BigInteger(a).multiply(den).add(new BigInteger(f));
                r = reduce(num, den);
            } else {
                if (!body.matches("[0-9]+")) {
                    throw new IllegalArgumentException("整数形状不合法");
                }
                r = reduce(new BigInteger(body), BigInteger.ONE);
            }
            return neg ? reduce(r.n.negate(), r.d) : r;
        }

        public String canon() {
            return d.equals(BigInteger.ONE) ? n.toString() : n + "/" + d;
        }

        @Override
        public int compareTo(Num o) {
            return n.multiply(o.d).compareTo(o.n.multiply(d));
        }
    }

    private static final Pattern TIME = Pattern.compile(
            "(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(Z|[+-]\\d{2}:\\d{2})");

    public static boolean validDate(String s) {
        if (!s.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        int y = Integer.parseInt(s.substring(0, 4));
        int m = Integer.parseInt(s.substring(5, 7));
        int d = Integer.parseInt(s.substring(8, 10));
        if (y < 1 || y > 9999 || m < 1 || m > 12 || d < 1 || d > 31) {
            return false;
        }
        try {
            return java.time.LocalDate.of(y, m, d).getDayOfMonth() == d;
        } catch (java.time.DateTimeException e) {
            return false;
        }
    }

    public static OffsetDateTime parseTime(String s) {
        Matcher m = TIME.matcher(s);
        if (!m.matches()) {
            return null;
        }
        int y = Integer.parseInt(m.group(1));
        int mo = Integer.parseInt(m.group(2));
        int d = Integer.parseInt(m.group(3));
        int h = Integer.parseInt(m.group(4));
        int mi = Integer.parseInt(m.group(5));
        int sec = Integer.parseInt(m.group(6));
        if (y < 1 || y > 9999 || h > 23 || mi > 59 || sec > 59) {
            return null;
        }
        if (!validDate(String.format("%04d-%02d-%02d", y, mo, d))) {
            return null;
        }
        String off = m.group(7);
        ZoneOffset zo;
        if ("Z".equals(off)) {
            zo = ZoneOffset.UTC;
        } else {
            int oh = Integer.parseInt(off.substring(1, 3));
            int om = Integer.parseInt(off.substring(4, 6));
            if (oh > 14 || om > 59) {
                return null;
            }
            if (oh == 14 && om != 0) {
                return null;
            }
            int total = oh * 3600 + om * 60;
            if (off.charAt(0) == '-') {
                total = -total;
            }
            zo = ZoneOffset.ofTotalSeconds(total);
        }
        return OffsetDateTime.of(y, mo, d, h, mi, sec, 0, zo);
    }

    public static boolean validInterval(String start, String end) {
        OffsetDateTime a = parseTime(start);
        OffsetDateTime b = parseTime(end);
        return a != null && b != null && a.isBefore(b);
    }

    public static boolean validUrl(String s) {
        String lower = s.toLowerCase();
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= 0x20 || c == 0x7f) {
                return false;
            }
            if (c == '%') {
                if (i + 2 >= s.length()) {
                    return false;
                }
                if (!isHex(s.charAt(i + 1)) || !isHex(s.charAt(i + 2))) {
                    return false;
                }
            }
        }
        String rest = s.substring(s.indexOf("://") + 3);
        int end = rest.length();
        for (char sep : new char[]{'/', '?', '#'}) {
            int k = rest.indexOf(sep);
            if (k >= 0 && k < end) {
                end = k;
            }
        }
        return end > 0;
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public static String decode(String raw, List<String> errors) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '\\') {
                if (i + 1 >= raw.length()) {
                    errors.add("字符串以孤立反斜杠结尾");
                    return null;
                }
                char e = raw.charAt(i + 1);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u': {
                        if (i + 6 > raw.length()) {
                            errors.add("\\u 后必须跟四位十六进制数");
                            return null;
                        }
                        String hex = raw.substring(i + 2, i + 6);
                        if (!hex.matches("[0-9A-Fa-f]{4}")) {
                            errors.add("\\u 后必须跟四位十六进制数");
                            return null;
                        }
                        sb.append((char) Integer.parseInt(hex, 16));
                        i += 6;
                        continue;
                    }
                    default:
                        errors.add("非法转义 \\" + e);
                        return null;
                }
                i += 2;
            } else {
                if (c < 0x20 || c == 0x7f) {
                    errors.add("字符串中出现未转义的控制字符");
                    return null;
                }
                sb.append(c);
                i++;
            }
        }
        String out = sb.toString();
        for (int k = 0; k < out.length(); k++) {
            char c = out.charAt(k);
            if (Character.isHighSurrogate(c)) {
                if (k + 1 < out.length() && Character.isLowSurrogate(out.charAt(k + 1))) {
                    k++;
                } else {
                    errors.add("字符串中出现孤立的高位代理项");
                    return null;
                }
            } else if (Character.isLowSurrogate(c)) {
                errors.add("字符串中出现孤立的低位代理项");
                return null;
            }
        }
        return out;
    }

}
