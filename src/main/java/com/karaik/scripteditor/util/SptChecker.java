package com.karaik.scripteditor.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class SptChecker {
    private static class Gb2312Holder {
        static final Set<Character> INSTANCE = loadSet();
    }

    public static boolean checkChar(Character c) {
        return Gb2312Holder.INSTANCE.contains(c);
    }

    public static boolean checkSpt(String input) {
        if (input == null) return false;
        Set<Character> gb2312 = Gb2312Holder.INSTANCE;
        for (char c : input.toCharArray()) {
            if (!gb2312.contains(c)) return false;
        }
        return true;
    }

    private static Set<Character> loadSet() {
        Set<Character> set = new HashSet<>(7445);
        try (InputStream in = SptChecker.class.getResourceAsStream("/com/karaik/scripteditor/gb2312_valid_characters.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                set.add(line.charAt(0));
            }
        } catch (Exception e) {
            System.err.println("加载GB2312字符集失败: " + e.getMessage());
        }
        return set;
    }
}
