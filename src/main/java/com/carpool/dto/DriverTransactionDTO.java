package com.carpool.dto;

import java.time.LocalDateTime;

public class DriverTransactionDTO {

    private Long bookingId;
    private String passengerName;
    private String source;
    private String destination;
    private LocalDateTime date;
    private int seats;
    private double amount;
    private String transactionId;

    public DriverTransactionDTO(
            Long bookingId,
            String passengerName,
            String source,
            String destination,
            LocalDateTime date,
            int seats,
            double amount,
            String transactionId
    ) {
        this.bookingId = bookingId;
        this.passengerName = passengerName;
        this.source = source;
        this.destination = destination;
        this.date = date;
        this.seats = seats;
        this.amount = amount;
        this.transactionId = transactionId;
    }

    public Long getBookingId() { return bookingId; }
    public String getPassengerName() { return passengerName; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public LocalDateTime getDate() { return date; }
    public int getSeats() { return seats; }
    public double getAmount() { return amount; }
    public String getTransactionId() { return transactionId; }
}
