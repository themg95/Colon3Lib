package dev.mg95.colon3lib;

import dev.mg95.colon3lib.config.Config;
import dev.mg95.colon3lib.config.Option;

public class Colon3Config extends Config {
    @Option
    public boolean isGay = true;

    public Colon3Config() {
        init(this, "colon3lib");
    }

}
