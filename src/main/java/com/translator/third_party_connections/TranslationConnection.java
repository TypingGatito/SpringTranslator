package com.translator.third_party_connections;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;


@Component
public class TranslationConnection {
    private RestTemplate restTemplate;

    private String source;

    @Autowired
    public TranslationConnection(@Qualifier("translationUrl") String source, RestTemplate restTemplate) {
        this.source = source;
        this.restTemplate = restTemplate;
    }

    public String translateWord(String from, String to, String word) {
        if (word.equals("")) return word;
        String apiUrl = source + "/translate" + "?sl=" + from
                + "&dl=" + to + "&text=" + word;

        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
        String bodyJSON = response.getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(bodyJSON);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String value = rootNode.path("destination-text").asText();

        return value;
    }

    public BiMap<String, String> getPossibleLanguages() {
        ResponseEntity<String> response = restTemplate.getForEntity(source + "/languages", String.class);
        String bodyJSON = response.getBody();

        ObjectMapper objectMapper = new ObjectMapper();

        BiMap<String, String> biMap;
        try {
            Map<String, String> map = objectMapper.readValue(bodyJSON, Map.class);
            biMap = HashBiMap.create();
            for(Map.Entry<String, String> entry : map.entrySet()) {
                String entryKey = entry.getKey();
                String entryValue = entry.getValue();
                if (biMap.containsValue(entryValue)) entryValue += "1";
                biMap.put(entryKey, entryValue);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return biMap;
    }

}
