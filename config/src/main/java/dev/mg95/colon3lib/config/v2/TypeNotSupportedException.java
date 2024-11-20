package dev.mg95.colon3lib.config.v2;

public class TypeNotSupportedException extends RuntimeException {
    public TypeNotSupportedException(Class<?> type) {
        super(type + " is not supported in Colon3Lib");
    }
}
