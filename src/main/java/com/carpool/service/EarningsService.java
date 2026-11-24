package com.carpool.service;

import com.carpool.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EarningsService {

    private final BookingRepository bookingRepository;

    public EarningsService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // ✅ Last X days earnings
    public Double getLastDaysEarnings(String email, int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return bookingRepository.findLastDaysEarnings(email, fromDate);
    }

    // ✅ Monthly breakdown
    public List<Map<String, Object>> getMonthlyBreakdown(String email) {

        List<Object[]> rows = bookingRepository.findMonthlyEarningsByDriver(email);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] r : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("year", r[0]);
            map.put("month", r[1]);
            map.put("amount", r[2]);
            result.add(map);
        }

        return result;
    }
}
