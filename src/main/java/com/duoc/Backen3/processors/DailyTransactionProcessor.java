package com.duoc.Backen3.processors;

import com.duoc.Backen3.domain.DailyTransaction;

import org.springframework.batch.item.ItemProcessor;

import java.time.LocalTime;

// processor encargado de validar y detectar anomalias en transacciones diarias
public class DailyTransactionProcessor
        implements ItemProcessor<DailyTransaction, DailyTransaction> {

    // hora de inicio permitida para transacciones
    private final LocalTime start = LocalTime.of(8, 0);

    // hora de termino permitida para transacciones
    private final LocalTime end = LocalTime.of(20, 0);

    // metodo principal que evalua cada transaccion diaria
    @Override
    public DailyTransaction process(DailyTransaction item) {

        // indicador de anomalia
        boolean anomaly = false;

        // acumulador de razones de anomalia
        StringBuilder reason = new StringBuilder();

        // validacion de monto obligatorio
        if (item.getAmount() == null) {
        throw new IllegalArgumentException("amount is null");
        }

        // regla 1: monto negativo
        if (item.getAmount().signum() < 0) {
        anomaly = true;
        reason.append("negative_amount;");
        }

        // obtencion de la hora de la transaccion
        LocalTime t = item.getTxTimestamp().toLocalTime();

        // regla 2: transaccion fuera del horario permitido
        if (t.isBefore(start) || t.isAfter(end)) {
        anomaly = true;
        reason.append("out_of_hours;");
        }

        // asignacion de resultados al objeto
        item.setAnomaly(anomaly);
        item.setAnomalyReason(anomaly ? reason.toString() : null);

        // retorna la transaccion procesada
        return item;
    }
}
