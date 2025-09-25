package de.bbajor.pvs.intravitreal.treatment.dto;

public enum State {

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

    State(String code, String value) {
        this.code = code;
        this.value = value;
    }

    public static State byString(String code) {
        for (State bundesland : State.values()) {
            if (bundesland.code.equals(code)) {
                return bundesland;
            }
        }
        return State.NB;
    }

    @Override
    public String toString() {
        return value;
    }
}
