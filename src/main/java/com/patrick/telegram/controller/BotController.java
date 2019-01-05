package com.patrick.telegram.controller;

import com.patrick.telegram.model.Bot;
import com.patrick.telegram.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Created by Patrick on 17.03.2018.
 */

@RestController()
@RequestMapping("/bots")
public class BotController {
    private final BotService botService;

    @Autowired
    public BotController(BotService botService) {
        this.botService = botService;
    }

    @GetMapping
    public Collection<Bot> getBots() {
        return botService.getBots();
    }

    @PostMapping
    public void saveBot(@RequestBody Bot bot) {
        botService.saveBot(bot);
    }
}