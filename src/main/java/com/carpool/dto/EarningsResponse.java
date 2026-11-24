// dto/EarningsResponse.java
package com.carpool.dto;

import java.util.Map;

public class EarningsResponse {
    private double total;
    private double thisMonth;
    private double thisYear;
    private Map<String, Double> monthlyBreakdown;

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public double getThisMonth() { return thisMonth; }
    public void setThisMonth(double thisMonth) { this.thisMonth = thisMonth; }

    public double getThisYear() { return thisYear; }
    public void setThisYear(double thisYear) { this.thisYear = thisYear; }

    public Map<String, Double> getMonthlyBreakdown() { return monthlyBreakdown; }
    public void setMonthlyBreakdown(Map<String, Double> monthlyBreakdown) {
        this.monthlyBreakdown = monthlyBreakdown;
    }
}
