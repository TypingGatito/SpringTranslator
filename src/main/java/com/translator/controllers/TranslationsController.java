package com.translator.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import com.translator.third_party_connections.TranslationConnection;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.translator.DAO.TranslationDAO;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/rest_api")
public class TranslationsController {

    private TranslationConnection translationConnection;

    private final TranslationDAO translationDAO;


    private ExecutorService executor = Executors.newFixedThreadPool(10);

    @Autowired
    public TranslationsController(TranslationDAO translationDAO, TranslationConnection translationConnection) {
        this.translationDAO = translationDAO;
        this.translationConnection = translationConnection;
    }

    @PostMapping("/translate")
    public ResponseEntity<Map<String, String>> translate(HttpServletRequest request) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, String> map = new HashMap<>();
        map.put("translation", "");

        String text;
        String from;
        String to;

        ObjectMapper objectMapper = new ObjectMapper();
        BiMap<String, String> languages = translationConnection.getPossibleLanguages();

        try {
            String bodyJSON = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonNode rootNode = objectMapper.readTree(bodyJSON);

            String rawFrom = rootNode.path("from").asText();
            String rawTo = rootNode.path("to").asText();
            text = rootNode.path("text").asText();


            if (languages.containsValue(rawFrom)) from = languages.inverse().get(rawFrom);
            else from = rawFrom;
            if (languages.containsValue(rawTo)) to = languages.inverse().get(rawTo);
            else to = rawTo;
        } catch (IOException e) {
            map.put("translation", "Internal Server Error");
            ResponseEntity<Map<String, String>> response = new ResponseEntity<>(map, headers, HttpStatus.INTERNAL_SERVER_ERROR);

            return response;
        }

        if (!(languages.containsKey(from))) {
            map.put("translation", "Not found 'from' language");

            return new ResponseEntity<>(map, headers, HttpStatus.BAD_REQUEST);
        }

        if (!(languages.containsKey(to))) {
            map.put("translation", "Not found 'to' language");

            return new ResponseEntity<>(map, headers, HttpStatus.BAD_REQUEST);
        }

        String[] words = text.split(" ");
        StringBuffer translatedText = new StringBuffer();

        List<Future<String>> futures = new ArrayList<>();

        for (String word : words) {
            Future<String> future = executor.submit(() -> {
                if (word.trim().equals("\n") || word.equals("")) return word;
                String translated = translationConnection.translateWord(from,
                        to,
                        word.replaceAll("\n", "%0A").replaceAll(" ", "%20"));

                translated = translated.replaceAll("%0А", "%0A").replaceAll("\\п ", "\n");

                return translated.replaceAll("%0A", "\n").replaceAll("%20", " ");
            });
            futures.add(future);
        }

        for (Future<String> future : futures) {
            try {
                String translatedWord = future.get();
                translatedText.append(translatedWord).append(" ");
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        map.put("translation", translatedText.toString());

        String ipAddress = request.getRemoteAddr();

        translationDAO.update(ipAddress, text, translatedText.toString());

        return new ResponseEntity<>(map, headers, HttpStatus.OK);
    }

    @PostMapping("/translateT")
    public ResponseEntity<String> translateT(HttpServletRequest request) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=UTF-8");

        String bodyString = "";

        String text;
        String from;
        String to;

        BiMap<String, String> languages = translationConnection.getPossibleLanguages();

        try {
            BufferedReader reader = request.getReader();
            StringBuilder textB = new StringBuilder();
            String inp = reader.readLine().trim();
            String rawFrom = inp.split("->")[0];
            String rawTo = inp.split("->")[1];
            while ((inp = reader.readLine()) != null) textB.append(inp);
            text = textB.toString();


            if (languages.containsValue(rawFrom)) from = languages.inverse().get(rawFrom);
            else from = rawFrom;
            if (languages.containsValue(rawTo)) to = languages.inverse().get(rawTo);
            else to = rawTo;
        } catch (IOException e) {
            bodyString =  "Internal Server Error";

            return new ResponseEntity<>(bodyString, headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (!(languages.containsKey(from))) {
            bodyString =  "Not found 'from' language";

            return new ResponseEntity<>(bodyString, headers, HttpStatus.BAD_REQUEST);
        }

        if (!(languages.containsKey(to))) {
            bodyString =  "Not found 'to' language";

            return new ResponseEntity<>(bodyString, headers, HttpStatus.BAD_REQUEST);
        }

        String[] words = text.split(" ");
        StringBuffer translatedText = new StringBuffer();

        List<Future<String>> futures = new ArrayList<>();

        for (String word : words) {
            Future<String> future = executor.submit(() -> {
                if (word.trim().equals("\n") || word.equals("")) return word;
                String translated = translationConnection.translateWord(from,
                        to,
                        word.replaceAll("\n", "%0A").replaceAll(" ", "%20"));

                translated = translated.replace("%0А", "%0A").replaceAll("\\п ", "\n");

                return translated.replace("%0A", "\n").replaceAll("%20", " ");
            });
            futures.add(future);
        }

        for (Future<String> future : futures) {
            try {
                String translatedWord = future.get();
                translatedText.append(translatedWord).append(" ");
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        bodyString = translatedText.toString();

        String ipAddress = request.getRemoteAddr();

        translationDAO.update(ipAddress, text, translatedText.toString());

        return new ResponseEntity<>(bodyString, headers, HttpStatus.OK);
    }

    @GetMapping("/languages")
    public Map<String, String> languages() {
        return translationConnection.getPossibleLanguages();
    }

}
