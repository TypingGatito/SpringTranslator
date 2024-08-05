package com.translator.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.translator.models.TranslationRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.RestTemplate;
import com.translator.DAO.TranslationDAO;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@Controller
public class TranslationsControllerWeb {

    private RestTemplate restTemplate;

    private final TranslationDAO translationDAO;


    private ExecutorService executor = Executors.newFixedThreadPool(10);

    @Autowired
    public TranslationsControllerWeb(TranslationDAO translationDAO, RestTemplate restTemplate) {
        this.translationDAO = translationDAO;
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public String index(Model model) throws InterruptedException {
        model.addAttribute("request", new TranslationRequest());

        return "translation/translation";
    }

    @GetMapping("/translate")
    public String translate(@ModelAttribute("request") TranslationRequest translationRequest,
                            HttpServletRequest request) throws InterruptedException {
        String[] words = translationRequest.getText().split(" ");
        StringBuilder translatedText = new StringBuilder();

        List<Future<String>> futures = new ArrayList<>();

        for (String word : words) {
            Future<String> future = executor.submit(() -> {
                String apiUrl = "https://ftapi.pythonanywhere.com//translate?sl=" + translationRequest.getFrom()
                        + "&dl=" + translationRequest.getTo() + "&text=" + word;

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

        translationRequest.setTranslatedText(translatedText.toString());

        translationDAO.update(ipAddress, translationRequest.getText(), translatedText.toString());

        return "translation/translation";
    }

}
