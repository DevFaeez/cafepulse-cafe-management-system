package com.cafemanagement.cafeprediction.service;

import com.cafemanagement.cafeprediction.repository.CustomerLoyaltyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CustomerLoyaltyDashboard {
    private static final int PAGE_SIZE = 15;
    private final CustomerLoyaltyRepository customers;
    private final ZoneId zone;

    public CustomerLoyaltyDashboard(CustomerLoyaltyRepository customers, @Value("${cafe.business-zone}") String zone) {
        this.customers = customers;
        this.zone = ZoneId.of(zone);
    }

    public Directory directory(String input, int page) {
        String search = input == null ? "" : input.replaceAll("[ ()-]", "");
        if (!search.matches("\\+?[0-9]*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search using a customer phone number.");
        }
        if (search.startsWith("+60")) search = "0" + search.substring(3);
        long total = customers.countMatching(search);
        int current = (int) Math.min(page, Math.max(0, (total - 1) / PAGE_SIZE));
        var entries = customers.directory(search, PAGE_SIZE, (long) current * PAGE_SIZE).stream()
                .map(row -> new Contact(row.getId(), row.getPhone(), row.getJoinedAt().toLocalDate(),
                        row.getPaidOrders(), row.getAvailablePoints())).toList();
        return new Directory(current, PAGE_SIZE, total, entries);
    }

    public Overview overview() {
        ZonedDateTime now = ZonedDateTime.now(zone);
        YearMonth last = YearMonth.from(now);
        YearMonth first = last.minusMonths(5);
        var totals = customers.overview(now.minusDays(30).toInstant());
        var registrations = customers.registrations(first.atDay(1).atStartOfDay(), LocalDateTime.from(now)).stream()
                .collect(Collectors.toMap(CustomerLoyaltyRepository.MonthCount::getMonthKey,
                        CustomerLoyaltyRepository.MonthCount::getTotal));
        List<Registration> months = new ArrayList<>();
        for (YearMonth month = first; !month.isAfter(last); month = month.plusMonths(1)) {
            months.add(new Registration(month.toString(), registrations.getOrDefault(month.toString(), 0L)));
        }
        long customers = totals.getTotalCustomers();
        long one = totals.getSinglePurchase();
        long repeat = totals.getReturningCustomers();
        long under100 = totals.getPointsUnder100();
        long atLeast100 = totals.getPointsAtLeast100();
        return new Overview(customers, repeat, totals.getActiveCustomers(), totals.getTotalPoints(), months,
                new PurchaseMix(customers - one - repeat, one, repeat),
                new PointsMix(customers - under100 - atLeast100, under100, atLeast100));
    }

    public record Contact(long id, String phone, java.time.LocalDate joinedOn, long paidOrders, int availablePoints) {}
    public record Directory(int page, int pageSize, long total, List<Contact> customers) {}
    public record Overview(long totalCustomers, long returningCustomers, long activeCustomers, long availablePoints,
                           List<Registration> registrations, PurchaseMix purchases, PointsMix points) {}
    public record Registration(String month, long count) {}
    public record PurchaseMix(long none, long once, long repeat) {}
    public record PointsMix(long none, long under100, long atLeast100) {}
}
