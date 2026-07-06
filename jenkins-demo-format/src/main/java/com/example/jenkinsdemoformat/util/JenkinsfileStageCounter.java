package com.example.jenkinsdemoformat.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JenkinsfileStageCounter {

    private static final Pattern STAGE_PATTERN = Pattern.compile(
            "(?i)stage\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)",
            Pattern.MULTILINE
    );

    public static int countStages(String jenkinsfile) {
        if (jenkinsfile == null || jenkinsfile.isEmpty()) {
            return 0;
        }
        int count = 0;
        Matcher matcher = STAGE_PATTERN.matcher(jenkinsfile);
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}