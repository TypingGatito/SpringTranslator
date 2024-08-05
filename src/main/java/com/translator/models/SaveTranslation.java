package com.translator.models;

import lombok.Data;

@Data
public class SaveTranslation {
    private Long id;

    private String ipAddress;
    private String inputText;
    private String translatedText;
}
