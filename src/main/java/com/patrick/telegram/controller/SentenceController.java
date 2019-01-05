package com.patrick.telegram.controller;

import com.patrick.telegram.model.Sentence;
import com.patrick.telegram.service.SentenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Created by Patrick on 26.01.2018.
 */
@RestController()
@RequestMapping("/sentences")
public class SentenceController {

    private final SentenceService sentenceService;

    @Autowired
    public SentenceController(SentenceService sentenceService) {
        this.sentenceService = sentenceService;
    }

    @GetMapping
    public Collection<Sentence> getSentences() {
        return sentenceService.getSentences();
    }

    @GetMapping("/{id}")
    public Sentence getSentence(@PathVariable int id) {
        return sentenceService.getSentence(id);
    }

    @DeleteMapping("/{id}")
    public void deleteSentence(@PathVariable int id) {
        sentenceService.deleteSentence(id);
    }

    @PostMapping
    public void addSentence(@RequestBody Sentence sentence) {
        sentenceService.addSentence(sentence);
    }
}