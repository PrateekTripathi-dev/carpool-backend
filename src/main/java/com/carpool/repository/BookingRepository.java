//src/main/java/com/carpool/repository/BookingRepository.java
package com.carpool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.carpool.model.Booking;
import java.util.List;


public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByRequesterId(Long requesterId);
    List<Booking> findByRideId(Long rideId);
    boolean existsByTransactionId(String transactionId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE Booking b SET b.status = :status WHERE b.id = :id")
    int updateStatusById(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("status") Booking.Status status);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE Booking b SET b.status = :status WHERE b.rideId = :rideId AND b.status IN :currentStatuses")
    int updateStatusByRideIdAndCurrentStatuses(@org.springframework.data.repository.query.Param("rideId") Long rideId, @org.springframework.data.repository.query.Param("status") Booking.Status status, @org.springframework.data.repository.query.Param("currentStatuses") java.util.List<Booking.Status> currentStatuses);
    // 🔥 Driver earnings - month wise
    @org.springframework.data.jpa.repository.Query("""
SELECT 
  FUNCTION('YEAR', b.paymentCompletedAt),
  FUNCTION('MONTH', b.paymentCompletedAt),
  SUM(r.price * b.seatsRequested)
FROM Booking b
JOIN Ride r ON b.rideId = r.id
WHERE r.ownerEmail = :email
  AND b.status IN ('CONFIRMED','COMPLETED')
  AND b.paymentCompletedAt IS NOT NULL
GROUP BY FUNCTION('YEAR', b.paymentCompletedAt), FUNCTION('MONTH', b.paymentCompletedAt)
ORDER BY FUNCTION('YEAR', b.paymentCompletedAt) DESC, FUNCTION('MONTH', b.paymentCompletedAt) DESC
""")
    List<Object[]> findMonthlyEarningsByDriver(@org.springframework.data.repository.query.Param("email") String email);


    // 🔥 Driver earnings - total for a year
    @org.springframework.data.jpa.repository.Query("""
SELECT COALESCE(SUM(r.price * b.seatsRequested),0)
FROM Booking b
JOIN Ride r ON b.rideId = r.id
WHERE r.ownerEmail = :email
  AND b.status IN ('CONFIRMED','COMPLETED')
  AND b.paymentCompletedAt IS NOT NULL
  AND FUNCTION('YEAR', b.paymentCompletedAt) = :year
""")
    Double findYearlyEarningsByDriver(
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("year") int year
    );


    // 🔥 Driver earnings - current month
    @org.springframework.data.jpa.repository.Query("""
SELECT COALESCE(SUM(r.price * b.seatsRequested),0)
FROM Booking b
JOIN Ride r ON b.rideId = r.id
WHERE r.ownerEmail = :email
  AND b.status IN ('CONFIRMED','COMPLETED')
  AND b.paymentCompletedAt IS NOT NULL
  AND FUNCTION('YEAR', b.paymentCompletedAt) = :year
  AND FUNCTION('MONTH', b.paymentCompletedAt) = :month
""")
    Double findCurrentMonthEarnings(
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("year") int year,
            @org.springframework.data.repository.query.Param("month") int month
    );
    // 🔥 Driver earnings - last X days
    @org.springframework.data.jpa.repository.Query("""
SELECT COALESCE(SUM(r.price * b.seatsRequested),0)
FROM Booking b
JOIN Ride r ON b.rideId = r.id
WHERE r.ownerEmail = :email
  AND b.status IN ('CONFIRMED','COMPLETED')
  AND b.paymentCompletedAt IS NOT NULL
  AND b.paymentCompletedAt >= :fromDate
""")
    Double findLastDaysEarnings(
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate
    );


}
