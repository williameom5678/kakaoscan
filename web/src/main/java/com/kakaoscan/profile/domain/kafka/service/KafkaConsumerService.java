package com.kakaoscan.profile.domain.kafka.service;

import com.kakaoscan.profile.domain.enums.KafkaEventType;
import com.kakaoscan.profile.domain.kafka.config.KafkaProperties;
import com.kakaoscan.profile.domain.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final ApplicationEventPublisher eventPublisher;

    @KafkaListener(topics = {KafkaProperties.TOPIC_EVENT}, groupId = KafkaProperties.GROUP_EVENT, containerFactory = "eventTypeMapListenerContainerFactory")
    public void onMessageEventTypeMap(ConsumerRecord<KafkaEventType, Map<String, Object>> record, Acknowledgment ack) {
        try {

            KafkaEventType eventType = KafkaEventType.valueOf(String.valueOf(record.key()).split(":")[0].toUpperCase());
            Map<String, Object> map = record.value();

            switch (eventType) {
                case SCAN_AFTER_EVENT:
                    KafkaEvent kafkaScanAfterEvent = new KafkaScanAfterEvent(map);
                    eventPublisher.publishEvent(kafkaScanAfterEvent);
                    break;

                case SEND_MAIL_EVENT:
                    KafkaEvent kafkaSendMailEvent = new KafkaSendMailEvent(map);
                    eventPublisher.publishEvent(kafkaSendMailEvent);
                    break;

                case RECORD_LOG_EVENT:
                    KafkaEvent kafkaRecordLogEvent = new KafkaRecordLogEvent(map);
                    eventPublisher.publishEvent(kafkaRecordLogEvent);
                    break;

                case MESSAGE_BOT_EVENT:
                    KafkaEvent kafkaMessageBotEvent = new KafkaMessageBotEvent(map);
                    eventPublisher.publishEvent(kafkaMessageBotEvent);
                    break;

                default:
                    log.error("invalid kafka event : {}", eventType);
                    break;
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("consumer onMessage error : {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = {KafkaProperties.TOPIC_SERVER_EVENT}, groupId = KafkaProperties.GROUP_SERVER_EVENT, containerFactory = "stringStringListenerContainerFactory")
    public void onMessageStringString(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {

            KafkaEventType eventType = KafkaEventType.valueOf(String.valueOf(record.key()).split(":")[0].toUpperCase());

            switch (eventType) {
                case ALERT_SERVER_ERROR_EVENT:
                    KafkaEvent kafkaMessageBotEvent = KafkaMessageBotEvent.builder()
                            .message(record.value())
                            .build();
                    eventPublisher.publishEvent(kafkaMessageBotEvent);
                    break;

                default:
                    log.error("invalid kafka event : {}", eventType);
                    break;
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("consumer onMessage error : {}", e.getMessage(), e);
        }
    }

    @Bean
    public ErrorHandler eventTypeMapErrorHandler(KafkaTemplate<KafkaEventType, Map<String, Object>> kafkaTemplate) {
        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        FixedBackOff backOff = new FixedBackOff(1000L, 1L); // n초 대기, n번 재시도
        return new SeekToCurrentErrorHandler(deadLetterPublishingRecoverer, backOff);
    }

    @Bean
    public ErrorHandler stringStringErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        FixedBackOff backOff = new FixedBackOff(1000L, 1L);
        return new SeekToCurrentErrorHandler(deadLetterPublishingRecoverer, backOff);
    }
}
