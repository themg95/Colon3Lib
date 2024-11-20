package dev.mg95.colon3lib.config.v2;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DoubleSlider {
    double min();
    double max();
}
