package com.translator.controllers;

import com.translator.models.TranslationRequest;
import com.translator.third_party_connections.TranslationConnection;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.translator.DAO.TranslationDAO;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Controller
public class TranslationsControllerWeb {

    private TranslationConnection translationConnection;

    private final TranslationDAO translationDAO;


    private ExecutorService executor = Executors.newFixedThreadPool(10);

    @Autowired
    public TranslationsControllerWeb(TranslationDAO translationDAO, TranslationConnection translationConnection) {
        this.translationDAO = translationDAO;
        this.translationConnection = translationConnection;
    }

    @GetMapping
    public String translate(@ModelAttribute("request") TranslationRequest translationRequest,
                            HttpServletRequest request) throws InterruptedException {
        if (translationRequest == null ||
            translationRequest.getFrom() == null|| translationRequest.getTo() == null ||
            translationRequest.getText() == null) return "translation/translation";
        String[] words = translationRequest.getText().split(" ");
        StringBuilder translatedText = new StringBuilder();

        List<Future<String>> futures = new ArrayList<>();

        for (String word : words) {
            Future<String> future = executor.submit(() -> {
                return translationConnection.translateWord(translationRequest.getFrom(),
                        translationRequest.getTo(),
                        word);
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
