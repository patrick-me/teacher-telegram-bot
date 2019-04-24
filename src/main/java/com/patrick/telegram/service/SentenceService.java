package com.patrick.telegram.service;

import com.patrick.telegram.model.Question;
import com.patrick.telegram.model.Sentence;
import com.patrick.telegram.repository.QuestionRepository;
import com.patrick.telegram.repository.SentenceRepository;
import com.patrick.telegram.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by Patrick on 26.02.2018.
 */
@Service
@Transactional
public class SentenceService {
    private final SentenceRepository sentenceRepository;
    private final UserSessionRepository userSessionRepository;
    private final QuestionRepository questionRepository;

    @Autowired
    public SentenceService(SentenceRepository sentenceRepository, UserSessionRepository userSessionRepository, QuestionRepository questionRepository) {
        this.sentenceRepository = sentenceRepository;
        this.userSessionRepository = userSessionRepository;
        this.questionRepository = questionRepository;
    }

    public Collection<Sentence> getSentences() {
        return sentenceRepository.findAll();
    }

    public Sentence getSentence(int id) {
        return sentenceRepository.findOne(id);
    }

    public void addSentence(Sentence sentence) {
        filterEmptyQuestions(sentence);
        sentenceRepository.save(sentence);
    }

    //TODO: check & clear user session when remove the question type of the sentence

    private void filterEmptyQuestions(Sentence sentence) {
        Collection<Question> questions = sentence.getQuestions();

        sentence.setQuestions(
                questions.stream().filter(
                        q -> !StringUtils.isEmpty(q.getHighlightedSentence()) &&
                                !StringUtils.isEmpty(q.getKeyboard()) &&
                                !StringUtils.isEmpty(q.getQuestion())
                ).collect(Collectors.toList())
        );
    }

    public void deleteSentence(int id) {
        userSessionRepository.deleteBySentenceId(id);
        questionRepository.deleteBySentenceId(id);
        sentenceRepository.deleteSentence(id);
    }

    public Collection<Sentence> getSentences(int lessonId) {
        return sentenceRepository.getSentences(lessonId);
    }
}