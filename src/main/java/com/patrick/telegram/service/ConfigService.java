package com.patrick.telegram.service;

import com.patrick.telegram.model.Config;
import com.patrick.telegram.repository.ConfigRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class ConfigService {
    private final static String NUMBER_HOW_OFTEN_SEND_PANDAS_KEY = "numberHowOftenSendPandas";
    private final static String NUMBER_HOW_OFTEN_SEND_PANDAS_VALUE = "5";

    private final ConfigRepository configRepository;

    public ConfigService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public String getCommandDescription(String command) {
        Config config = configRepository.get(command);
        if(config == null) {
            configRepository.save(new Config(command, "", true));
            return "";
        }

        return config.getValue();
    }

    public int getNumberHowOftenSendPandas() {
        return Integer.valueOf(getConfig(NUMBER_HOW_OFTEN_SEND_PANDAS_KEY).getValue());
    }

    public Config getConfig(String name) {
        switch (name) {
            case NUMBER_HOW_OFTEN_SEND_PANDAS_KEY:
                return Optional.ofNullable(configRepository.get(name))
                        .orElse(
                                new Config(NUMBER_HOW_OFTEN_SEND_PANDAS_KEY, NUMBER_HOW_OFTEN_SEND_PANDAS_VALUE, false)
                        );
            default:
                throw new RuntimeException("Config is not found");
        }
    }

    public void saveConfig(Config config) {
        configRepository.save(config);
    }
}
