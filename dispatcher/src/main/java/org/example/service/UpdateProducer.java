package org.example.service;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public interface UpdateProducer {
    void produce(String rabbitQueue, Update update);
}
