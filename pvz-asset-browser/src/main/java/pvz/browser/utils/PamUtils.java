package pvz.browser.utils;

public class PamUtils {
    public static String getPamNameFromPath(String path) {
        String[] segments = path.split("/");
        String name = segments.length == 0 ? path : segments[segments.length - 1];
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(0, dot);
        }
        return name;
    }

    public static String getPamCategoryFromPath(String path) {
        String[] segments = path.split("/");
        return segments.length >= 3 ? PamUtils.prettify(segments[2]) : "Other";
    }

    public static String getPrettiyPamNameFromPath(String path) {
        return PamUtils.prettify(getPamNameFromPath(path));
    }

    public static String prettify(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        boolean newWord = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '_' || c == ' ') {
                if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') {
                    out.append(' ');
                }
                newWord = true;
            } else {
                out.append(newWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
                newWord = false;
            }
        }
        return out.toString();
    }
}
