package com.patrick.telegram.controller;

import com.patrick.telegram.model.Panda;
import com.patrick.telegram.service.PandaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Created by Patrick on 25.01.2018.
 */
@RestController()
@RequestMapping("/pandas")
public class PandaController {

    private final PandaService pandaService;

    @Autowired
    public PandaController(PandaService pandaService) {
        this.pandaService = pandaService;
    }

    @GetMapping
    public Collection<Panda> getPandas() {
        return pandaService.getPandas();
    }

    @PostMapping
    public void addPanda(@RequestBody Panda panda) {
        pandaService.addPanda(panda);
    }
}