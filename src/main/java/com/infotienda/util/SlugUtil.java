package com.infotienda.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
public class SlugUtil {
    public static String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String slug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return slug.toLowerCase().replaceAll("\s+", "-");
    }
}
