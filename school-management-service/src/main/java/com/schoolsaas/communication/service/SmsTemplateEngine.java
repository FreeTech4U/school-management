package com.schoolsaas.communication.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SmsTemplateEngine {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    public String resolve(String template, Map<String, String> variables) {
        if (template == null) return null;
        
        StringBuilder sb = new StringBuilder();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(template, lastEnd, matcher.start());
            String key = matcher.group(1);
            String value = variables.get(key);
            
            if (value != null) {
                sb.append(value);
            } else {
                log.warn("Missing value for variable '{}' in SMS template", key);
                sb.append(matcher.group(0)); // Keep {{key}} if not found
            }
            lastEnd = matcher.end();
        }
        sb.append(template.substring(lastEnd));

        String result = sb.toString();
        if (result.length() > 160) {
            log.warn("SMS message exceeds 160 characters ({} chars)", result.length());
        }
        
        return result;
    }
}
