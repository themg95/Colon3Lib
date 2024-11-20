package dev.mg95.colon3lib;

import dev.mg95.colon3lib.config.v2.ConfigModel;
import dev.mg95.colon3lib.config.v2.DoubleSlider;
import dev.mg95.colon3lib.config.v2.Nested;
import dev.mg95.colon3lib.config.v2.Slider;

@ConfigModel(id = "colon3lib", name = "Colon3LibConfig")
public class Colon3LibConfigModel {
    public boolean isGay = true;
    public String sex = "gay";
    public int howgay = 100;
    @Nested
    public static class amog {
        public boolean sus = true;
        @Nested
        public static class amog2 {
            public boolean sus = true;
        }
    }
    @Slider(min = 0, max = 100)
    public int howgaypercent = 100;

    @DoubleSlider(min = 1.5, max = 2)
    public double howgayprecise = 1.5;

    public double howgayfree = 1.5;


}
