package com.carpool.controller;
import com.carpool.dto.DriverTransactionDTO;
import com.carpool.model.Booking;
import com.carpool.model.Ride;
import com.carpool.repository.BookingRepository;
import com.carpool.repository.RideRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/driver")
@CrossOrigin
public class DriverTransactionController {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;

    public DriverTransactionController(
            BookingRepository bookingRepository,
            RideRepository rideRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
    }

    /**
     * DRIVER TRANSACTION HISTORY
     * Shows only CONFIRMED + PAID bookings for driver's rides
     */
    @GetMapping("/transactions")
    public List<DriverTransactionDTO> getDriverTransactions(
            @RequestParam String driverEmail
    ) {

        List<Ride> driverRides = rideRepository.findByOwnerEmail(driverEmail);
        List<DriverTransactionDTO> transactions = new ArrayList<>();

        for (Ride ride : driverRides) {

            List<Booking> bookings = bookingRepository.findByRideId(ride.getId());

            for (Booking booking : bookings) {

                // Include bookings that have a transaction (transactionId) and are either CONFIRMED or PAID or have paymentCompletedAt set
                if (booking.getTransactionId() != null && (
                        booking.getStatus() == Booking.Status.CONFIRMED ||
                        booking.getStatus() == Booking.Status.PAID ||
                        booking.getPaymentCompletedAt() != null
                )) {

                    int seats = booking.getSeatsRequested() > 0 ? booking.getSeatsRequested() : 1;
                    double base = ride.getPrice() * seats;
                    double platformFee = 20.0 * seats; // Rs 20 per seat taken by platform
                    double amount = base - platformFee;

                    transactions.add(
                            new DriverTransactionDTO(
                                    booking.getId(),
                                    booking.getRequesterName(),        // Passenger
                                    ride.getFromLocation(),            // Source
                                    ride.getToLocation(),              // Destination
                                    booking.getPaymentCompletedAt(),   // Date (IMPORTANT)
                                    booking.getSeatsRequested(),
                                    amount,
                                    booking.getTransactionId()
                            )
                    );

                }
            }
        }

        return transactions;
    }
}

