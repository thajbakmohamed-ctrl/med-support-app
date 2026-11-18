
package com.medsupport.booking_service.repository;

import com.medsupport.booking_service.model.BookingOutboxEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

public interface BookingOutboxRepository
        extends JpaRepository<BookingOutboxEvent, UUID> {
    @Query(value = """
                    SELECT *
                    FROM booking_outbox_events
                    WHERE status = 'PENDING'
                    ORDER BY created_at, event_id
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<BookingOutboxEvent> findPendingEventsForUpdate(
            @Param("batchSize") int batchSize
    );
}
