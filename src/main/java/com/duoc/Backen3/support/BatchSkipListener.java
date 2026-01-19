package com.duoc.Backen3.support;

import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.SkipListener;

// listener para registrar eventos de omision durante el procesamiento batch
@Slf4j
public class BatchSkipListener<T, S>
        implements SkipListener<T, S> {

    // se ejecuta cuando ocurre un error durante la lectura
    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("skip in read: {}", t.getMessage());
    }

    // se ejecuta cuando ocurre un error durante la escritura
    @Override
    public void onSkipInWrite(S item, Throwable t) {
        log.warn("skip in write item={} error={}", item, t.getMessage());
    }

    // se ejecuta cuando ocurre un error durante el procesamiento
    @Override
    public void onSkipInProcess(T item, Throwable t) {
        log.warn("skip in process item={} error={}", item, t.getMessage());
    }
}
