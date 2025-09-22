package de.bbajor.pvs.ivomplan.dto;

public enum Bundesland {

    BW("BW", "Baden-Württemberg"),
    BY("BY", "Bayern"),
    BE("BE", "Berlin"),
    BB("BB", "Brandenburg"),
    HB("HB", "Bremen"),
    HH("HH", "Hamburg"),
    HE("HE", "Hessen"),
    MV("MV", "Mecklenburg-Vorpommern"),
    NI("NI", "Niedersachsen"),
    NW("NW", "Nordrhein-Westfalen"),
    RP("RP", "Rheinland-Pfalz"),
    SL("SL", "Saarland"),
    SN("SN", "Sachsen"),
    ST("ST", "Sachsen-Anhalt"),
    SH("SH", "Schleswig-Holstein"),
    TH("TH", "Thüringen"),
    NB("NB", "Bundesland n.b.");

    private final String code;
    private final String value;

    Bundesland(String code, String value) {
        this.code = code;
        this.value = value;
    }

    public static Bundesland byString(String code) {
        for (Bundesland bundesland : Bundesland.values()) {
            if (bundesland.code.equals(code)) {
                return bundesland;
            }
        }
        return Bundesland.NB;
    }

    @Override
    public String toString() {
        return value;
    }
}
