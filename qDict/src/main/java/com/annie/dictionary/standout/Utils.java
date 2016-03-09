package com.annie.dictionary.standout;

public class Utils {
    public static boolean isSet(int flags, int flag) {
        return (flags & flag) == flag;
    }
}
