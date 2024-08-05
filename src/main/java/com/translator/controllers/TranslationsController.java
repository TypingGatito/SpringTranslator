package com.translator.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.translator.DAO.TranslationDAO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/rest_api")
public class TranslationsController {

    private RestTemplate restTemplate;

    private final TranslationDAO translationDAO;


    private ExecutorService executor = Executors.newFixedThreadPool(10);

    @Autowired
    public TranslationsController(TranslationDAO translationDAO, RestTemplate restTemplate) {
        this.translationDAO = translationDAO;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/translate")
    public HashMap<String, String> translate(@RequestParam("text") String text,
                             @RequestParam("from") String from,
                             @RequestParam("to") String to,
                             HttpServletRequest request) throws InterruptedException {
        String[] words = text.split(" ");
        StringBuilder translatedText = new StringBuilder();

        List<Future<String>> futures = new ArrayList<>();

        for (String word : words) {
            Future<String> future = executor.submit(() -> {
                String apiUrl = "https://ftapi.pythonanywhere.com//translate?sl=" + from + "&dl=" + to + "&text=" + word;

                String response = restTemplate.getForObject(apiUrl, String.class);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(response);

                String value = rootNode.path("destination-text").asText();

                return value;
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

        String ipAddress = request.getRemoteAddr();

        translationDAO.update(ipAddress, text, translatedText.toString());

        HashMap<String, String> map = new HashMap<>();
        map.put("text", translatedText.toString());

        return map;
    }

}
