package ml.justify.justify2.util;

public class Xss {
  public static String deXss(String input) {
    return input.replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#x27;");
  }
}
