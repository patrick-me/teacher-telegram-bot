package com.patrick.telegram.service;

import com.patrick.telegram.model.Panda;
import com.patrick.telegram.repository.PandaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;

@Service
@Transactional
public class PandaService {

    private final ConfigService configService;
    private final PandaRepository pandaRepository;

    @Autowired
    public PandaService(ConfigService configService, PandaRepository pandaRepository) {
        this.configService = configService;
        this.pandaRepository = pandaRepository;
    }

    //"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAPAAAADwCAYAAAA+VemSAAAgAEl...=="
    public InputStream getPandaInputStream(Panda panda) {
        String imageString = panda.getImageBase64().split(",")[1];
        byte[] imageByte = Base64.getDecoder().decode(imageString);
        return new ByteArrayInputStream(imageByte);
    }

    public Optional<Panda> getPositivePanda() {
        return Optional.ofNullable(pandaRepository.getPositiveRandomPanda());
    }

    public Optional<Panda> getNegativePanda() {
        return Optional.ofNullable(pandaRepository.getNegativeRandomPanda());
    }

    public int getNumberHowOftenSendPanda() {
        return configService.getNumberHowOftenSendPandas();
    }

    public Collection<Panda> getPandas() {
        return pandaRepository.findAll();
    }

    public void addPanda(Panda panda) {
        pandaRepository.save(panda);
    }
}
