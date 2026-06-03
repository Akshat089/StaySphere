package com.staysphere.booking_service.repository;

import com.staysphere.booking_service.entity.Booking;
import com.staysphere.booking_service.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByGuestId(Long guestId);

    List<Booking> findByPropertyId(Long propertyId);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.propertyId = :propertyId
            AND b.status IN :statuses
            AND b.checkInDate < :checkOutDate
            AND b.checkOutDate > :checkInDate
            """)
    List<Booking> findOverlappingBookings(
            @Param("propertyId") Long propertyId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("statuses") List<BookingStatus> statuses
    );

    @Query(value = "SELECT pg_advisory_xact_lock(:propertyId)", nativeQuery = true)
    void lockProperty(@Param("propertyId") Long propertyId);
}