package com.patrick.telegram.controller;

import com.patrick.telegram.model.QuestionType;
import com.patrick.telegram.service.QuestionService;
import com.patrick.telegram.service.QuestionTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by Patrick on 25.01.2018.
 */
@RestController()
@RequestMapping("/questionTypes")
public class QuestionTypeController {

    @Autowired
    QuestionTypeService questionTypeService;

    @GetMapping
    public Collection<QuestionType> getQuestionTypes() {
        return questionTypeService.getQuestionTypes().stream()
                .filter(questionType -> {
                    String questionTypeStub = QuestionService.QUESTION_STUB.getQuestionType().getName();
                    return !questionTypeStub.equals(questionType.getName());
                })
                .collect(Collectors.toList());
    }

    @PostMapping
    public void addQuestionType(@RequestBody QuestionType questionType) {
        questionTypeService.addQuestionType(questionType);
    }
}