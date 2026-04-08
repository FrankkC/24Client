package it.comi.a24client;

public class LogEntry {

    public enum Type { TX, OK, ERR, STATUS, POS, LOG, SYS }

    public final Type type;
    public final String timestamp;
    public final String text;

    public LogEntry(Type type, String timestamp, String text) {
        this.type = type;
        this.timestamp = timestamp;
        this.text = text;
    }

    public static Type classify(String raw) {
        if (raw == null)               return Type.SYS;
        if (raw.startsWith("> "))      return Type.TX;
        if (raw.startsWith("OK"))      return Type.OK;
        if (raw.startsWith("ERR"))     return Type.ERR;
        if (raw.startsWith("STATUS ")) return Type.STATUS;
        if (raw.startsWith("POS "))    return Type.POS;
        if (raw.startsWith("LOG "))    return Type.LOG;
        return Type.SYS;
    }

    public static int colorFor(Type type) {
        switch (type) {
            case TX:     return 0xFF4FC3F7; // azzurro
            case OK:     return 0xFF00E676; // verde
            case ERR:    return 0xFFFF5252; // rosso
            case STATUS: return 0xFFFFD740; // ambra
            case POS:    return 0xFFCE93D8; // viola chiaro
            case LOG:    return 0xFFE0E0E0; // grigio chiaro
            case SYS:    return 0xFF90A4AE; // blu-grigio
            default:     return 0xFFFFFFFF;
        }
    }
}
