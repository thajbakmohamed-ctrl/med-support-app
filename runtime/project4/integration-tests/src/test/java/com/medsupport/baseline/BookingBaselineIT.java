package com.medsupport.baseline;

import com.medsupport.booking_service.model.DonationBooking;
import com.medsupport.booking_service.repository.DonationBookingRepository;
import com.medsupport.booking_service.request.DonationBookingRequest;
import com.medsupport.booking_service.service.*;
import com.medsupport.blood_request_service.model.Hospital;
import com.medsupport.blood_request_service.repository.HospitalRepository;
import com.medsupport.blood_request_service.service.BloodRequestService;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingBaselineIT {
    final IsolatedDatabase db = new IsolatedDatabase();
    ConfigurableApplicationContext bookingA, bookingB, blood;
    final LocalDate date = LocalDate.now().plusDays(7);
    final LocalTime time = LocalTime.of(10, 0);

    @Configuration(proxyBeanMethods = false) @EnableAutoConfiguration
    @EntityScan(basePackageClasses = DonationBooking.class)
    @EnableJpaRepositories(basePackageClasses = DonationBookingRepository.class)
    @Import({DonationBookingService.class, AuditLogService.class, BookingOutboxService.class})
    static class BookingContext {}

    @Configuration(proxyBeanMethods = false) @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Hospital.class)
    @EnableJpaRepositories(basePackageClasses = HospitalRepository.class)
    @Import({BloodRequestService.class, com.medsupport.blood_request_service.service.AuditLogService.class})
    static class BloodContext {}

    ConfigurableApplicationContext context(Class<?> source) {
        SpringApplication app = new SpringApplication(source);
        app.setWebApplicationType(WebApplicationType.NONE);
        var environment = new org.springframework.core.env.StandardEnvironment();
        environment.getPropertySources().remove("systemEnvironment");
        environment.getPropertySources().remove("systemProperties");
        app.setEnvironment(environment);
        return app.run("--spring.config.location=optional:classpath:/baseline-no-external-config/",
                "--spring.datasource.url=" + db.postgres.getJdbcUrl(),
                "--spring.datasource.username=" + db.postgres.getUsername(),
                "--spring.datasource.password=" + db.postgres.getPassword(),
                "--spring.datasource.hikari.connection-init-sql=SET lock_timeout = '5s'",
                "--spring.jpa.hibernate.ddl-auto=validate", "--spring.sql.init.mode=never",
                "--spring.jpa.open-in-view=false", "--spring.jpa.show-sql=false",
                "--spring.main.banner-mode=off", "--logging.level.root=WARN");
    }

    @BeforeAll void start() throws Exception {
        db.start();
        bookingA = context(BookingContext.class);
        bookingB = context(BookingContext.class);
        blood = context(BloodContext.class);
    }
    @AfterAll void stop() {
        for (var c : Arrays.asList(bookingA, bookingB, blood)) if (c != null) c.close();
        db.close();
    }
    @BeforeEach void seed() {
        db.jdbc.execute("TRUNCATE booking_outbox_events, donation_booking, donation_slot, blood_request, donor_profile, users, hospital, system_audit_logs RESTART IDENTITY CASCADE");
        db.jdbc.update("INSERT INTO hospital(id,name,location,phone,is_active) VALUES(1,'Synthetic hospital','Test','10000000',true)");
        for (long id = 1; id <= 8; id++) {
            db.jdbc.update("INSERT INTO users(id,name,email,password_hash,phone,role,status,email_verified) VALUES(?, 'Synthetic donor', ?, 'unused-test-hash', ?, 'DONOR','ACTIVE',true)", id, "donor"+id+"@example.invalid", String.valueOf(20000000+id));
            db.jdbc.update("INSERT INTO donor_profile(id,user_id,blood_type,date_of_birth,gender,weight_kg,cpr_image,created_at_donor_profile) VALUES(?,?,'O+',DATE '1990-01-01','TEST',70,'synthetic',CURRENT_TIMESTAMP)", id,id);
        }
        db.jdbc.update("INSERT INTO blood_request(id,hospital_id,blood_type,units_needed,urgency,needed_date,status) VALUES(1,1,'O+',2,'HIGH',?,'OPEN')",date);
        db.jdbc.update("INSERT INTO donation_slot(id,hospital_id,slot_date,start_time,end_time,capacity) VALUES(1,1,?,TIME '09:00',TIME '12:00',1)",date);
    }
    DonationBookingService service(ConfigurableApplicationContext c) { return c.getBean(DonationBookingService.class); }
    long create(ConfigurableApplicationContext c, long donor) {
        DonationBookingRequest r = new DonationBookingRequest();
        r.setDonorProfileId(donor); r.setBloodRequestId(1L); r.setDonationSlotId(1L);
        r.setDonationBookingDate(date); r.setDonationBookingTime(time);
        return service(c).createBooking(r, donor, 1L, date, "OPEN", "donor"+donor+"@example.invalid").id();
    }
    int count(String table) { return db.jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class); }
    int reserved() { return db.jdbc.queryForObject("SELECT reserved_count FROM donation_slot WHERE id=1",Integer.class); }
    String status() { return db.jdbc.queryForObject("SELECT status FROM blood_request WHERE id=1",String.class); }
    void invariant() {
        assertEquals(db.jdbc.queryForObject("SELECT count(*) FROM donation_booking WHERE donation_booking_status <> 'CANCELLED'",Integer.class),reserved());
        assertTrue(reserved() >= 0);
        assertTrue(reserved() <= db.jdbc.queryForObject("SELECT capacity FROM donation_slot WHERE id=1",Integer.class));
    }
    // Every worker enters an independent proxied transaction. Never @Transactional on tests.
    List<Object> race(List<Supplier<?>> actions) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(actions.size());
        CountDownLatch ready = new CountDownLatch(actions.size()), go = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Supplier<?> action : actions) futures.add(pool.submit(() -> {
                ready.countDown(); if (!go.await(10,TimeUnit.SECONDS)) throw new AssertionError("Start timeout");
                try { return action.get(); } catch (org.springframework.web.server.ResponseStatusException e) { return e; }
            }));
            assertTrue(ready.await(10,TimeUnit.SECONDS)); go.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> f : futures) results.add(f.get(25,TimeUnit.SECONDS));
            return results;
        } finally { go.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS)); }
    }
    @Test void multipleDonorsCompeteForLastPlaceAcrossTwoInstances() throws Exception {
        List<Supplier<?>> actions = new ArrayList<>();
        for (long donor=1; donor<=8; donor++) { long id=donor; actions.add(() -> create(id%2==0?bookingA:bookingB,id)); }
        List<Object> results = race(actions);
        assertEquals(1,results.stream().filter(Long.class::isInstance).count());
        for (Object r : results) if (r instanceof org.springframework.web.server.ResponseStatusException e) assertEquals(409,e.getStatusCode().value());
        assertEquals(1,count("donation_booking")); assertEquals(1,count("system_audit_logs")); assertEquals(1,count("booking_outbox_events")); invariant();
    }
    @Test void duplicateRuleIsSameDonorExactDateAndTime() {
        db.jdbc.update("UPDATE donation_slot SET capacity=3 WHERE id=1"); create(bookingA,1);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->create(bookingB,1));
        assertEquals(1,count("donation_booking")); invariant();
    }
    @Test void differentTimeIsAllowedAndCancellationAllowsRebooking() {
        db.jdbc.update("UPDATE donation_slot SET capacity=3 WHERE id=1");
        long first=create(bookingA,1);
        DonationBookingRequest r=new DonationBookingRequest();
        r.setDonorProfileId(1L); r.setBloodRequestId(1L); r.setDonationSlotId(1L);
        r.setDonationBookingDate(date); r.setDonationBookingTime(time.plusMinutes(15));
        service(bookingB).createBooking(r,1L,1L,date,"OPEN","donor1@example.invalid");
        service(bookingA).cancelBooking(first,1L,"donor1@example.invalid");
        create(bookingB,1);
        assertEquals(3,count("donation_booking")); assertEquals(2,reserved()); invariant();
    }
    @Test void concurrentCancellationAndConfirmationReleaseCapacityOnce() throws Exception {
        long id=create(bookingA,1);
        race(List.of(() -> service(bookingA).cancelBooking(id,1L,"donor1@example.invalid"),
                () -> service(bookingB).updateBookingStatus(id,"CONFIRMED",1L,"staff@example.invalid",1L,"donor1@example.invalid")));
        assertEquals("CANCELLED",db.jdbc.queryForObject("SELECT donation_booking_status FROM donation_booking WHERE id=?",String.class,id));
        assertEquals(0,reserved()); invariant();
    }
    @Test void concurrentCancellationAndCompletionHaveOneTerminalOutcome() throws Exception {
        db.jdbc.update("UPDATE blood_request SET units_needed=1 WHERE id=1");
        long id=create(bookingA,1);
        service(bookingA).updateBookingStatus(id,"CONFIRMED",1L,"staff@example.invalid",1L,"donor1@example.invalid");
        race(List.of(() -> service(bookingA).cancelBooking(id,1L,"donor1@example.invalid"),
                () -> service(bookingB).updateBookingStatus(id,"COMPLETED",1L,"staff@example.invalid",1L,"donor1@example.invalid")));
        String terminal=db.jdbc.queryForObject("SELECT donation_booking_status FROM donation_booking WHERE id=?",String.class,id);
        assertTrue(Set.of("CANCELLED","COMPLETED").contains(terminal));
        assertEquals(terminal.equals("COMPLETED")?1:0,reserved());
        assertEquals(terminal.equals("COMPLETED")?"COMPLETED":"OPEN",status());
        assertEquals(3,count("booking_outbox_events")); invariant();
    }
    @Test void creationRacingClosureHasSerializedOutcome() throws Exception {
        race(List.of(() -> create(bookingA,1), () -> blood.getBean(BloodRequestService.class).updateBloodRequestStatus(1L,"CLOSED",1L,"staff@example.invalid")));
        assertEquals("CLOSED",status()); assertTrue(count("donation_booking")<=1); invariant();
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->create(bookingB,2));
    }
    @Test void creationRacingDeletionCannotLeaveOrphanOrLoseReservation() throws Exception {
        race(List.of(() -> create(bookingA,1), () -> { blood.getBean(BloodRequestService.class).deleteBloodRequest(1L,1L,"staff@example.invalid"); return "deleted"; }));
        assertEquals(count("donation_booking"),count("blood_request")); invariant();
    }
    @Test void concurrentCompletionsAtomicallyCompleteRequestAndKeepReservations() throws Exception {
        db.jdbc.update("UPDATE donation_slot SET capacity=2 WHERE id=1");
        long a=create(bookingA,1), b=create(bookingB,2);
        service(bookingA).updateBookingStatus(a,"CONFIRMED",1L,"staff@example.invalid",1L,"donor1@example.invalid");
        service(bookingB).updateBookingStatus(b,"CONFIRMED",1L,"staff@example.invalid",2L,"donor2@example.invalid");
        List<Object> result=race(List.of(
                ()->service(bookingA).updateBookingStatus(a,"COMPLETED",1L,"staff@example.invalid",1L,"donor1@example.invalid"),
                ()->service(bookingB).updateBookingStatus(b,"COMPLETED",1L,"staff@example.invalid",2L,"donor2@example.invalid")));
        assertFalse(result.stream().anyMatch(Throwable.class::isInstance));
        assertEquals("COMPLETED",status()); assertEquals(2,reserved());
        assertEquals(1,db.jdbc.queryForObject("SELECT count(*) FROM system_audit_logs WHERE audit_action='BLOOD_REQUEST_COMPLETED'",Integer.class));
        assertEquals(6,count("booking_outbox_events")); invariant();
    }
    @Test void databaseFailureRollsBackBookingCapacityAuditAndOutbox() {
        db.jdbc.execute("ALTER TABLE booking_outbox_events ADD CONSTRAINT baseline_injected_failure CHECK (notification_type <> 'BOOKING_CREATED')");
        try {
            // Flush outbox inside outer transaction: force the real DB failure after all writes.
            TransactionTemplate tx = new TransactionTemplate(bookingA.getBean(org.springframework.transaction.PlatformTransactionManager.class));
            assertThrows(RuntimeException.class,()->tx.execute(s->{create(bookingA,1); bookingA.getBean(com.medsupport.booking_service.repository.BookingOutboxRepository.class).flush(); return null;}));
            assertEquals(0,count("donation_booking")); assertEquals(0,reserved());
            assertEquals(0,count("system_audit_logs")); assertEquals(0,count("booking_outbox_events")); assertEquals("OPEN",status());
        } finally { db.jdbc.execute("ALTER TABLE booking_outbox_events DROP CONSTRAINT baseline_injected_failure"); }
    }
    @Test void failureAfterAutomaticCompletionRollsBackBothStatusesAndEvents() {
        db.jdbc.update("UPDATE blood_request SET units_needed=1 WHERE id=1");
        long id=create(bookingA,1);
        service(bookingA).updateBookingStatus(id,"CONFIRMED",1L,"staff@example.invalid",1L,"donor1@example.invalid");
        int auditBefore=count("system_audit_logs"), eventsBefore=count("booking_outbox_events");
        db.jdbc.execute("ALTER TABLE booking_outbox_events ADD CONSTRAINT baseline_injected_failure CHECK (notification_type <> 'BOOKING_COMPLETED')");
        try {
            TransactionTemplate tx=new TransactionTemplate(bookingA.getBean(org.springframework.transaction.PlatformTransactionManager.class));
            assertThrows(RuntimeException.class,()->tx.execute(s->{
                service(bookingA).updateBookingStatus(id,"COMPLETED",1L,"staff@example.invalid",1L,"donor1@example.invalid");
                bookingA.getBean(com.medsupport.booking_service.repository.BookingOutboxRepository.class).flush(); return null;
            }));
            assertEquals("OPEN",status());
            assertEquals("CONFIRMED",db.jdbc.queryForObject("SELECT donation_booking_status FROM donation_booking WHERE id=?",String.class,id));
            assertEquals(auditBefore,count("system_audit_logs")); assertEquals(eventsBefore,count("booking_outbox_events")); invariant();
        } finally { db.jdbc.execute("ALTER TABLE booking_outbox_events DROP CONSTRAINT baseline_injected_failure"); }
    }
}
