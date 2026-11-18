package com.medsupport.baseline;

import com.medsupport.booking_service.event.BookingNotificationEvent;
import com.medsupport.booking_service.producer.BookingEventProducer;
import com.medsupport.notification_service.consumer.BookingEventConsumer;
import com.medsupport.notification_service.service.NotificationService;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.*;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaNotificationIT {
    @Test void actualProducerSendsKeyedEventThroughDisposableBrokerToConsumerContract() throws Exception {
        try(var kafka=new KafkaContainer("apache/kafka:3.9.1").withReuse(false)) {
            kafka.start();
            String bootstrap=kafka.getBootstrapServers();
            try(var admin=AdminClient.create(Map.of("bootstrap.servers",bootstrap))) {
                admin.createTopics(List.of(new NewTopic("booking-events",1,(short)1))).all().get(20,TimeUnit.SECONDS);
            }
            var factory=new DefaultKafkaProducerFactory<String,BookingNotificationEvent>(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrap,ProducerConfig.ACKS_CONFIG,"all",ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,true),
                    new StringSerializer(),new JacksonJsonSerializer<>());
            var template=new KafkaTemplate<>(factory);
            try(var consumer=new KafkaConsumer<String,com.medsupport.notification_service.event.BookingNotificationEvent>(Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrap,ConsumerConfig.GROUP_ID_CONFIG,UUID.randomUUID().toString(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest",ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false),
                        new StringDeserializer(),new JacksonJsonDeserializer<>(com.medsupport.notification_service.event.BookingNotificationEvent.class,false))) {
                consumer.subscribe(List.of("booking-events"));
                new BookingEventProducer(template,"booking-events").publishBookingEvent(NotificationEventContractTest.fixture()).get(20,TimeUnit.SECONDS);
                List<ConsumerRecord<String,com.medsupport.notification_service.event.BookingNotificationEvent>> records=new ArrayList<>();
                long deadline=System.nanoTime()+Duration.ofSeconds(20).toNanos();
                while(records.isEmpty()&&System.nanoTime()<deadline) consumer.poll(Duration.ofMillis(250)).forEach(records::add);
                assertEquals(1,records.size()); assertEquals("7",records.get(0).key());
                NotificationService service=mock(NotificationService.class);
                new BookingEventConsumer(service).consumeBookingEvent(records.get(0).value());
                verify(service).createNotificationFromEvent(NotificationEventContractTest.EVENT,"donor@example.invalid","BOOKING_CREATED","Donation Booking Created","Synthetic test message",7L);
            } finally { template.destroy(); factory.destroy(); }
        }
    }
}
