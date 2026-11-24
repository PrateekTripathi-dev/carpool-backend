package com.carpool.service;

import com.carpool.model.Booking;
import com.carpool.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingCompletionService {

    private final BookingRepository bookingRepository;

    @Transactional
    public int completeBookingsForRide(Long rideId) {

        List<Booking.Status> allowedCurrentStatuses =
                List.of(Booking.Status.CONFIRMED);

        return bookingRepository.updateStatusByRideIdAndCurrentStatuses(
                rideId,
                Booking.Status.COMPLETED,
                allowedCurrentStatuses
        );
    }
}
