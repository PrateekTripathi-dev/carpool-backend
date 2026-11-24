package com.carpool.model;

import com.carpool.repository.BookingRepository;
import com.carpool.repository.RideRepository;
import com.carpool.model.Booking;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class RideCompletionScheduler {

    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;

    public RideCompletionScheduler(RideRepository rideRepository,
                                   BookingRepository bookingRepository) {
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
    }

    // Runs every 10 minutes
    @Scheduled(cron = "0 */10 * * * *")
    public void markCompletedRides() {

        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        List<Long> endedRideIds = rideRepository.findEndedRideIds(now);

        for (Long rideId : endedRideIds) {
            bookingRepository.updateStatusByRideIdAndCurrentStatuses(
                    rideId,
                    Booking.Status.COMPLETED,
                    List.of(Booking.Status.CONFIRMED)
            );
        }
    }
}
