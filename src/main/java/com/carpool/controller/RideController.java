// src/main/java/com/carpool/controller/RideController.java
package com.carpool.controller;

import com.carpool.model.Ride;
import com.carpool.repository.RideRepository;
import com.carpool.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for Ride-related operations.
 */
@RestController
@RequestMapping("/api/rides")
@CrossOrigin(origins = "*")
public class RideController {
    private static final Logger logger = LoggerFactory.getLogger(RideController.class);

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final com.carpool.repository.BookingRepository bookingRepository;
    private final com.carpool.repository.RatingReviewRepository ratingRepository;

    public RideController(RideRepository rideRepository, UserRepository userRepository, com.carpool.repository.BookingRepository bookingRepository, com.carpool.repository.RatingReviewRepository ratingRepository){
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.ratingRepository = ratingRepository;
    }

    @GetMapping
    public List<Ride> listAll(@RequestParam(required = false) String from,
                              @RequestParam(required = false) String to,
                              @RequestParam(required = false) Double fromLat,
                              @RequestParam(required = false) Double fromLng,
                              @RequestParam(required = false) Double toLat,
                              @RequestParam(required = false) Double toLng) {
        
        List<Ride> all = rideRepository.findAll();
        
        if (fromLat != null && fromLng != null && toLat != null && toLng != null) {
            return all.stream().filter(r -> {
                double distPickup = calculateDistance(r.getFromLat(), r.getFromLng(), fromLat, fromLng);
                double distDrop = calculateDistance(r.getToLat(), r.getToLng(), toLat, toLng);
                return distPickup <= 30.0 && distDrop <= 30.0;
            }).collect(Collectors.toList());
        }

        return all.stream().filter(r -> {
            boolean ok = true;
            if (from != null && !from.isBlank()) ok = r.getFromLocation().toLowerCase().contains(from.toLowerCase());
            if (ok && to != null && !to.isBlank()) ok = r.getToLocation().toLowerCase().contains(to.toLowerCase());
            return ok;
        }).collect(Collectors.toList());
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + 
                          Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            dist = dist * 1.609344;
            return (dist);
        }
    }

    private Optional<LocalDateTime> parseDateTime(String s) {
        if (s == null) return Optional.empty();
        try {
            return Optional.of(LocalDateTime.parse(s));
        } catch (Exception e) {
            try {
                return Optional.of(OffsetDateTime.parse(s).toLocalDateTime());
            } catch (Exception ex) {
                return Optional.empty();
            }
        }
    }

    @PostMapping
    public Ride create(@RequestBody Ride r){
        return rideRepository.save(r);
    }

    @GetMapping("/hosted")
    public List<Ride> hosted(HttpServletRequest request){
        String email = (String) request.getAttribute("email");
        if (email == null) {
            logger.warn("RideController.hosted: unauthenticated access");
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        // Fetch only rides owned by this user
        List<Ride> rides = rideRepository.findByOwnerEmail(email);

        // If ride time (or estimated completion) is in past, proactively mark eligible bookings as COMPLETED
        LocalDateTime now = LocalDateTime.now();
        java.util.List<com.carpool.model.Booking.Status> currentStatuses = java.util.Arrays.asList(
                com.carpool.model.Booking.Status.CONFIRMED,
                com.carpool.model.Booking.Status.ACCEPTED,
                com.carpool.model.Booking.Status.PAID
        );

        for (Ride r : rides) {
            Optional<LocalDateTime> maybeDt = parseDateTime(r.getEstimatedCompletionDateTime() != null && !r.getEstimatedCompletionDateTime().isBlank() ? r.getEstimatedCompletionDateTime() : r.getDateTime());
            if (maybeDt.isPresent() && maybeDt.get().isBefore(now)) {
                try {
                    int rows = bookingRepository.updateStatusByRideIdAndCurrentStatuses(r.getId(), com.carpool.model.Booking.Status.COMPLETED, currentStatuses);
                    if (rows > 0) {
                        logger.info("RideController.hosted: ride {} - marked {} bookings as COMPLETED", r.getId(), rows);
                    } else {
                        logger.debug("RideController.hosted: ride {} - no bookings required update", r.getId());
                    }
                } catch (Exception ex) {
                    logger.warn("RideController.hosted: failed to update bookings for ride {}", r.getId(), ex);
                }
            }
        }

        return rides;
    }

    @GetMapping("/{id}")
    public Ride get(@PathVariable Long id){ return rideRepository.findById(id).orElse(null); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (rideRepository.existsById(id)) {
            rideRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Allow host or admin to mark ALL CONFIRMED bookings for a ride as COMPLETED
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeRideBookings(@PathVariable Long id, HttpServletRequest request) {
        String email = (String) request.getAttribute("email");
        if (email == null) return ResponseEntity.status(401).build();

        Ride ride = rideRepository.findById(id).orElse(null);
        if (ride == null) return ResponseEntity.notFound().build();

        // Permission check: owner or admin/manager/assistant
        var userOpt = userRepository.findByEmail(email);
        boolean allowed = false;
        if (userOpt.isPresent()) {
            String role = userOpt.get().getRole();
            if (role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("MANAGER") || role.equalsIgnoreCase("ASSISTANT"))) allowed = true;
        }
        if (email.equals(ride.getOwnerEmail())) allowed = true;
        if (!allowed) return ResponseEntity.status(403).build();

        // Bulk update to mark eligible bookings as COMPLETED
        java.util.List<com.carpool.model.Booking.Status> currentStatuses = java.util.Arrays.asList(
                com.carpool.model.Booking.Status.CONFIRMED,
                com.carpool.model.Booking.Status.ACCEPTED,
                com.carpool.model.Booking.Status.PAID
        );
        int updated = bookingRepository.updateStatusByRideIdAndCurrentStatuses(ride.getId(), com.carpool.model.Booking.Status.COMPLETED, currentStatuses);

        return ResponseEntity.ok(Map.of("updated", updated));
    }

    // Idempotent endpoint: ensure bookings are marked COMPLETED when the ride's end time is in the past
    @PostMapping("/{id}/ensure-completed")
    public ResponseEntity<?> ensureCompleted(@PathVariable Long id, HttpServletRequest request) {
        String email = (String) request.getAttribute("email");
        if (email == null) return ResponseEntity.status(401).build();

        Ride ride = rideRepository.findById(id).orElse(null);
        if (ride == null) return ResponseEntity.notFound().build();

        // Permission check: owner or admin/manager/assistant
        var userOpt = userRepository.findByEmail(email);
        boolean allowed = false;
        if (userOpt.isPresent()) {
            String role = userOpt.get().getRole();
            if (role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("MANAGER") || role.equalsIgnoreCase("ASSISTANT"))) allowed = true;
        }
        if (email.equals(ride.getOwnerEmail())) allowed = true;
        if (!allowed) return ResponseEntity.status(403).build();

        // Verify ride end time has passed
        Optional<LocalDateTime> maybeDt = parseDateTime(ride.getEstimatedCompletionDateTime() != null && !ride.getEstimatedCompletionDateTime().isBlank() ? ride.getEstimatedCompletionDateTime() : ride.getDateTime());
        if (maybeDt.isEmpty() || maybeDt.get().isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(400).body(Map.of("message", "Ride end time has not passed yet"));
        }

        java.util.List<com.carpool.model.Booking.Status> currentStatuses = java.util.Arrays.asList(
                com.carpool.model.Booking.Status.CONFIRMED,
                com.carpool.model.Booking.Status.ACCEPTED,
                com.carpool.model.Booking.Status.PAID
        );

        // Defensive approach: try bulk update, but fall back to per-booking updates on failure
        int updated = 0;
        try {
            logger.debug("RideController.ensureCompleted: attempting bulk update for ride {}", ride.getId());
            int rows = bookingRepository.updateStatusByRideIdAndCurrentStatuses(ride.getId(), com.carpool.model.Booking.Status.COMPLETED, currentStatuses);
            updated += rows;
            logger.info("RideController.ensureCompleted: ride {} - bulk update marked {} bookings as COMPLETED", ride.getId(), rows);
        } catch (Exception ex) {
            logger.warn("RideController.ensureCompleted: bulk update failed for ride {}", ride.getId(), ex);
            try {
                java.util.List<com.carpool.model.Booking> bookings = bookingRepository.findByRideId(ride.getId());
                logger.debug("RideController.ensureCompleted: ride {} has {} bookings, details: {}", ride.getId(), bookings.size(), bookings.stream().map(b -> b.getId() + ":" + b.getStatus()).collect(java.util.stream.Collectors.joining(",")));
                for (com.carpool.model.Booking b : bookings) {
                    if (currentStatuses.contains(b.getStatus())) {
                        try {
                            int r = bookingRepository.updateStatusById(b.getId(), com.carpool.model.Booking.Status.COMPLETED);
                            if (r > 0) updated += r;
                        } catch (Exception ex2) {
                            logger.warn("RideController.ensureCompleted: failed updating booking {}", b.getId(), ex2);
                        }
                    }
                }
            } catch (Exception ex2) {
                logger.error("RideController.ensureCompleted: fallback per-booking update failed for ride {}", ride.getId(), ex2);
                return ResponseEntity.status(500).body(Map.of("message", "Failed to mark bookings as COMPLETED", "error", ex2.getMessage()));
            }
        }

        logger.info("RideController.ensureCompleted: ride {} - total bookings marked COMPLETED: {}", ride.getId(), updated);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    // Returns aggregate passenger ratings for a ride and per-booking ratings, and which bookings were rated by current user
    @GetMapping("/{id}/passenger-ratings")
    public ResponseEntity<?> passengerRatings(@PathVariable Long id, HttpServletRequest request) {
        try {
            var bookings = bookingRepository.findByRideId(id);
            var bookingIds = bookings.stream().map(b -> b.getId()).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
            if (bookingIds.isEmpty()) return ResponseEntity.ok(Map.of("average", null, "perBooking", Map.of(), "ratedByMe", Map.of()));

            java.util.List<com.carpool.model.RatingReview> ratings = ratingRepository.findByBookingIdIn(bookingIds);
            // defensive filtering: ignore any malformed entries
            ratings = ratings.stream().filter(r -> r != null && r.getBookingId() != null && r.getStars() >= 0 && r.getStars() <= 5).collect(java.util.stream.Collectors.toList());

            // average of ratings where reviewerRole == DRIVER (driver → passenger)
            java.util.OptionalDouble avgOpt = ratings.stream()
                    .filter(r -> r.getReviewerRole() == com.carpool.model.RatingReview.Role.DRIVER)
                    .mapToInt(com.carpool.model.RatingReview::getStars)
                    .average();
            Double avg = avgOpt.isPresent() ? Math.round(avgOpt.getAsDouble() * 10.0) / 10.0 : null;

            // per-booking averages (could be multiple raters)
            java.util.Map<Long, Double> perBooking = ratings.stream()
                    .collect(
                            java.util.stream.Collectors.groupingBy(
                                    com.carpool.model.RatingReview::getBookingId,
                                    java.util.stream.Collectors.averagingDouble(com.carpool.model.RatingReview::getStars)
                            )
                    );

            // which bookings were rated by current user
            String email = (String) request.getAttribute("email");
            java.util.Map<Long, Integer> ratedByMe = new java.util.HashMap<>();
            if (email != null) {
                var userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    Long me = userOpt.get().getId();
                    ratings.stream().filter(r -> me.equals(r.getReviewerId())).forEach(r -> ratedByMe.put(r.getBookingId(), r.getStars()));
                }
            }

            java.util.Map<String,Object> resp = new java.util.HashMap<>();
            resp.put("average", avg);
            resp.put("perBooking", perBooking);
            resp.put("ratedByMe", ratedByMe);
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            logger.error("passengerRatings: failed to compute passenger ratings for ride {}", id, ex);
            return ResponseEntity.status(500).body(Map.of("message", "Failed to compute passenger ratings", "error", ex.getMessage()));
        }
    }
}

