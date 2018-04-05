package com.patrick.telegram.service;

import com.patrick.telegram.model.Sentence;
import com.patrick.telegram.repository.SentenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collection;

/**
 * Created by Patrick on 26.02.2018.
 */
@Service
@Transactional
public class SentenceService {
    private final SentenceRepository sentenceRepository;

    @Autowired
    public SentenceService(SentenceRepository sentenceRepository) {
        this.sentenceRepository = sentenceRepository;
    }

    public Collection<Sentence> getSentences() {
        return sentenceRepository.findAll();
    }

    public Sentence getSentence(int id) {
        return sentenceRepository.findOne(id);
    }

    public void addSentence(Sentence sentence) {
        sentenceRepository.save(sentence);
    }
}