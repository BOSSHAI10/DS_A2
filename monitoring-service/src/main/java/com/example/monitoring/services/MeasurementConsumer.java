package com.example.monitoring.services;

import com.example.monitoring.dtos.MeasurementDTO;
import com.example.monitoring.entities.Measurement;
import com.example.monitoring.repositories.MeasurementRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MeasurementConsumer {

    @Autowired
    private MeasurementRepository measurementRepository;

    // Ascultă coada definită în RabbitMqConfig
    @RabbitListener(queues = "sensor_queue")
    public void consumeMessage(MeasurementDTO dto) {
        try {
            System.out.println("Message received from device: " + dto.getDevice_id());

            // Mapare DTO -> Entity
            Measurement measurement = new Measurement(
                    dto.getDevice_id(),
                    dto.getTimestamp(),
                    dto.getMeasurement_value()
            );

            // Salvare în DB
            measurementRepository.save(measurement);

            // AICI va trebui să adaugi logica de calcul orar (Requirements punctul 1)
            // checkHourlyConsumption(measurement);

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }
}