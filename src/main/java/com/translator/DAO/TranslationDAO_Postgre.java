package com.translator.DAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TranslationDAO_Postgre implements TranslationDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TranslationDAO_Postgre(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void update(String ipAddress, String text, String translatedText) {
        jdbcTemplate.update(
                "INSERT INTO translation_requests (ip_address, original_text, translated_text) VALUES (?, ?, ?)",
                new Object[]{ipAddress, text, translatedText}
                );
    }

}
