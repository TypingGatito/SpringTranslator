package com.translator.models;

import lombok.Data;

@Data
public class TranslationRequest {
    private String from;
    private String to;
    private String text;
    private String translatedText;
}
