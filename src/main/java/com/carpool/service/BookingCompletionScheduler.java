package com.carpool.service;

import com.carpool.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCompletionScheduler {

    private final RideRepository rideRepository;
    private final BookingCompletionService bookingCompletionService;

    @Scheduled(fixedDelay = 300_000)
    public void markCompletedBookings() {

        String now = LocalDateTime.now().toString();
        List<Long> completedRideIds = rideRepository.findEndedRideIds(now);

        for (Long rideId : completedRideIds) {
            try {
                int updated = bookingCompletionService.completeBookingsForRide(rideId);
                log.info("Ride {} → {} bookings marked COMPLETED", rideId, updated);
            } catch (Exception ex) {
                log.warn("Failed to complete bookings for ride {}: {}", rideId, ex.getMessage());
            }
        }
    }
}
