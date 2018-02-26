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

    @Autowired
    SentenceService sentenceService;

    @GetMapping
    public Collection<Sentence> getSentences() {
        return sentenceService.getSentences();
    }

    @GetMapping("/{id}")
    public Sentence getSentence(@PathVariable int id) {
        return sentenceService.getSentence(id);
    }

    @PostMapping
    public void addSentence(@RequestBody Sentence sentence) {
        sentenceService.addSentence(sentence);
    }
}