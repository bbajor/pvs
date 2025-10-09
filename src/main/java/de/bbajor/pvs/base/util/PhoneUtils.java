package de.bbajor.pvs.base.util;

public class PhoneUtils {

    public static String formatPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }

        // Entferne alle nicht-numerischen Zeichen
        String numbers = phone.replaceAll("[^0-9+]", "");

        // Wenn die Nummer mit 0 beginnt, ersetze sie durch +49
        if (numbers.startsWith("0")) {
            numbers = "+49" + numbers.substring(1);
        }
        // Wenn die Nummer ohne Ländervorwahl beginnt, füge +49 hinzu
        else if (!numbers.startsWith("+")) {
            numbers = "+49" + numbers;
        }

        return numbers;
    }
}
