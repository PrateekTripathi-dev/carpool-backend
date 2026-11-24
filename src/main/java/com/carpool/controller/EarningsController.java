package com.carpool.controller;

import com.carpool.repository.BookingRepository;
import com.carpool.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/earnings")
@CrossOrigin(origins = "*")
public class EarningsController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public EarningsController(BookingRepository bookingRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    // ✅ Monthly + Yearly + Breakdown
    @GetMapping("/driver")
    public Map<String, Object> getDriverEarnings(HttpServletRequest request) {

        String email = (String) request.getAttribute("email");
        if (email == null) return Map.of("error", "Unauthorized");

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        Double yearTotal = bookingRepository.findYearlyEarningsByDriver(email, year);
        Double monthTotal = bookingRepository.findCurrentMonthEarnings(email, year, month);

        List<Object[]> rows = bookingRepository.findMonthlyEarningsByDriver(email);
        List<Map<String, Object>> monthly = new ArrayList<>();

        for (Object[] r : rows) {
            monthly.add(Map.of(
                    "year", r[0],
                    "month", r[1],
                    "amount", r[2]
            ));
        }

        return Map.of(
                "currentMonth", monthTotal,
                "currentYear", yearTotal,
                "monthlyBreakdown", monthly
        );
    }

    // ✅ NEW: Last X days filter
    @GetMapping("/driver/last-days")
    public Map<String, Object> getLastDaysEarnings(
            @RequestParam int days,
            HttpServletRequest request
    ) {

        String email = (String) request.getAttribute("email");
        if (email == null) return Map.of("error", "Unauthorized");

        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);

        Double total = bookingRepository.findLastDaysEarnings(email, fromDate);

        return Map.of(
                "days", days,
                "total", total
        );
    }
}


