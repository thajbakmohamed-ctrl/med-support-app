
package com.medsupport.notification_service.config;

import com.medsupport.notification_service.event.BookingNotificationEvent;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public NewTopic bookingEventsDeadLetterTopic(
            @Value("${app.kafka.dlt-topic}") String dltTopic) {

        return TopicBuilder.name(dltTopic)
                .partitions(1)
                .replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, Object> dltProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers) {

        Map<String, Object> properties = new HashMap<>();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        Map<Class<?>, Serializer<?>> serializers = new HashMap<>();
        serializers.put(byte[].class, new ByteArraySerializer());

        serializers.put(BookingNotificationEvent.class,
                new JacksonJsonSerializer<BookingNotificationEvent>());

        return new DefaultKafkaProducerFactory<>(properties,
                new StringSerializer(), new DelegatingByTypeSerializer(serializers));
    }

    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate(
            ProducerFactory<String, Object> dltProducerFactory) {

        return new KafkaTemplate<>(dltProducerFactory);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> dltKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(dltKafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(record.topic() + "-dlt", -1));
        recoverer.setFailIfSendResultIsError(true);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(1000L, 2L));
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }
}
