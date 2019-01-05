package com.patrick.telegram.controller;

import com.patrick.telegram.model.QuestionType;
import com.patrick.telegram.service.QuestionTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

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
        return questionTypeService.getQuestionTypes();
    }

    @PostMapping
    public void addQuestionType(@RequestBody QuestionType questionType) {
        questionTypeService.addQuestionType(questionType);
    }
}