package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.report.SalesReportService;
import com.cafemanagement.cafeprediction.report.SalesReportService.Group;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('MANAGER')")
public class SalesReportController {
    private final SalesReportService reports;

    public SalesReportController(SalesReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/sales")
    public SalesReportService.Report sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") Group group) {
        return reports.report(from, to, group);
    }

    @GetMapping("/product-history")
    public SalesReportService.ProductHistory productHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.productHistory(from, to);
    }
}
