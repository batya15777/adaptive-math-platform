package com.adaptive.server.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Latency-diagnosis endpoints (public, no DB writes). Used to locate prod slowness:
 * /api/diag/ping isolates client→Render network + app overhead (no DB touched), while
 * /api/diag/db measures the Render→Railway round-trip, splitting connection-acquisition
 * cost from query cost. Compare the two (console + Network-tab TTFB) to tell whether the
 * bottleneck is the backend or the cross-region DB hop.
 */
@RestController
@RequestMapping("/api/diag")
public class DiagnosticsController {

    private final DataSource dataSource;

    public DiagnosticsController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** No DB. Baseline = client→Render RTT + JVM/app responsiveness. */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("serverEpochMs", System.currentTimeMillis());
        return body;
    }

    /** Times a trivial SELECT 1, splitting pool/connection acquisition from query execution. */
    @GetMapping("/db")
    public Map<String, Object> db() {
        Map<String, Object> body = new LinkedHashMap<>();
        long t0 = System.nanoTime();
        try (Connection c = dataSource.getConnection()) {
            long tConn = System.nanoTime();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT 1")) {
                rs.next();
            }
            long tQuery = System.nanoTime();
            body.put("ok", true);
            body.put("connectMs", (tConn - t0) / 1_000_000.0);
            body.put("queryMs", (tQuery - tConn) / 1_000_000.0);
            body.put("totalMs", (tQuery - t0) / 1_000_000.0);
        } catch (Exception e) {
            body.put("ok", false);
            body.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            body.put("totalMs", (System.nanoTime() - t0) / 1_000_000.0);
        }
        return body;
    }
}
