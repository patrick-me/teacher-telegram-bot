package com.patrick.telegram.controller;

import com.patrick.telegram.model.Config;
import com.patrick.telegram.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Created by Patrick on 31.12.2018.
 */
@RestController
@RequestMapping("/configs")
public class ConfigController {
    private final ConfigService configService;

    @Autowired
    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public Collection<Config> getConfigs() {
        return configService.getConfigs();
    }

    @GetMapping("/{name}")
    public Config getCommand(@PathVariable String name) {
        return configService.getConfig(name);
    }

    @PostMapping()
    public void saveConfig(@RequestBody Config config) {
        configService.saveConfig(config);
    }
}