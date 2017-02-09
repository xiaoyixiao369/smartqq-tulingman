package com.scienjus.smartqq;

import java.io.IOException;

/**
 * The code is sexy!
 * Created by gordon on 2017/2/8.
 */
public class TestApp {

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().exec(new String[] { "osascript", "-e", "display notification \"This is a message\" with title \"Title\" subtitle \"Subtitle\" sound name \"Funk\"" });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
