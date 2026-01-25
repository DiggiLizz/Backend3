package com.duoc.Backen3.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;

public class BatchSkipListener<T, S> implements SkipListener<T, S> {

    // Logger estándar sin depender de Lombok
    private static final Logger log = LoggerFactory.getLogger(BatchSkipListener.class);

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Error en lectura: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(S item, Throwable t) {
        log.warn("Error en escritura. Item: {} | Error: {}", item, t.getMessage());
    }

    @Override
    public void onSkipInProcess(T item, Throwable t) {
        log.warn("Error en proceso. Item: {} | Error: {}", item, t.getMessage());
    }
}