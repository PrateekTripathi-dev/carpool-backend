package com.carpool.controller;

import com.carpool.model.Booking;
import com.carpool.model.Ride;
import com.carpool.model.User;
import com.carpool.repository.BookingRepository;
import com.carpool.repository.RideRepository;
import com.carpool.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Import;
import com.carpool.config.TestMailConfig; 
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestMailConfig.class)
@AutoConfigureMockMvc
@Transactional
public class BookingControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Test
    public void hostCanCompleteBooking() throws Exception {
        User host = new User();
        host.setEmail("host@example.com");
        host.setFullname("Host");
        host.setRole("USER");
        userRepository.save(host);

        Ride ride = new Ride();
        ride.setOwnerEmail("host@example.com");
        ride.setDateTime(java.time.LocalDateTime.now().minusMinutes(5).toString());
        rideRepository.save(ride);

        Booking b = new Booking();
        b.setRideId(ride.getId());
        b.setRequesterEmail("pass@example.com");
        b.setStatus(Booking.Status.CONFIRMED);
        bookingRepository.save(b);

        mvc.perform(post("/api/bookings/" + b.getId() + "/complete").requestAttr("email", "host@example.com").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Clear persistence context so we see DB updates from the controller's transaction
        em.flush();
        em.clear();

        Booking after = bookingRepository.findById(b.getId()).orElse(null);
        assertEquals(Booking.Status.COMPLETED, after.getStatus());
    }

    @Test
    public void hostCanCompleteRideBookings() throws Exception {
        User host = new User();
        host.setEmail("host2@example.com");
        host.setFullname("Host2");
        host.setRole("USER");
        userRepository.save(host);

        Ride ride = new Ride();
        ride.setOwnerEmail("host2@example.com");
        ride.setDateTime(java.time.LocalDateTime.now().minusMinutes(5).toString());
        rideRepository.save(ride);

        Booking b1 = new Booking();
        b1.setRideId(ride.getId()); b1.setRequesterEmail("p1@example.com"); b1.setStatus(Booking.Status.CONFIRMED); bookingRepository.save(b1);
        Booking b2 = new Booking();
        b2.setRideId(ride.getId()); b2.setRequesterEmail("p2@example.com"); b2.setStatus(Booking.Status.CONFIRMED); bookingRepository.save(b2);
        Booking b3 = new Booking();
        b3.setRideId(ride.getId()); b3.setRequesterEmail("p3@example.com"); b3.setStatus(Booking.Status.PENDING); bookingRepository.save(b3);

        mvc.perform(post("/api/rides/" + ride.getId() + "/complete").requestAttr("email", "host2@example.com").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        // Ensure we see DB updates performed by controller
        em.flush();
        em.clear();

        Booking after1 = bookingRepository.findById(b1.getId()).orElse(null);
        Booking after2 = bookingRepository.findById(b2.getId()).orElse(null);
        Booking after3 = bookingRepository.findById(b3.getId()).orElse(null);
        assertEquals(Booking.Status.COMPLETED, after1.getStatus());
        assertEquals(Booking.Status.COMPLETED, after2.getStatus());
        assertEquals(Booking.Status.PENDING, after3.getStatus());
    }
}
