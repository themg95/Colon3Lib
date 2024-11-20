package dev.mg95.colon3lib;

public final class Colon3Lib {
    public static final String MOD_ID = "colon3lib";
    public static final dev.mg95.colon3lib.Colon3LibConfig CONFIG = new dev.mg95.colon3lib.Colon3LibConfig();

    public static void init() {
        CONFIG.load();
        if (!CONFIG.isGay()) {
            throw new NotGayEnoughException("Not gay enough!");
        }
        CONFIG.save();

    }
}
