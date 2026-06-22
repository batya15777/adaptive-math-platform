// Shared helpers + design tokens for the Admin "ML Groups" analytics dashboard.
// Pure data/string utilities — no JSX — so charts stay declarative.

import { COLORS } from "../../../pages/Dashboard/components/DashboardPrimitives.jsx";

// Cohesive, high-contrast palette for distinguishing clusters across all charts.
// Built around the app primary (#007bff) so it feels native to the product.
export const CLUSTER_PALETTE = [
    "#007bff", "#7c3aed", "#28a745", "#fd7e14",
    "#e83e8c", "#17a2b8", "#ffc107", "#6610f2",
];

// Deterministic per-cluster colour (same cluster → same colour in every chart).
export const clusterColor = (index) => CLUSTER_PALETTE[index % CLUSTER_PALETTE.length];

// 0..1 accuracy → whole-number percent, null-safe.
export const accuracyPct = (avgAccuracy) =>
    avgAccuracy == null ? null : Math.round(avgAccuracy * 100);

// Strips the raw cluster label down to its human name:
// "Needs strong support - mainly EMPTY_ANSWER" → "Needs strong support".
export const clusterName = (label, fallback) => {
    if (!label) return fallback;
    return label.split(" - mainly ")[0].trim() || fallback;
};

// Maps an error-pattern code to a localized, human label (falls back to a
// Title-Cased version of the raw code so unknown patterns still read cleanly).
export const humanizeError = (code, t) => {
    if (!code) return "";
    const known = {
        INVALID_FORMAT_ERROR: t.errInvalidFormat,
        EMPTY_ANSWER: t.errEmptyAnswer,
        MINOR_CALCULATION_ERROR: t.errMinorCalc,
        CONFUSED_SUB_WITH_ADD: t.errConfusedSubAdd,
    };
    if (known[code]) return known[code];
    const op = code.replace(/^GENERAL_ERROR_/, "").toLowerCase();
    const ops = { add: t.opAdd, sub: t.opSub, mult: t.opMult, div: t.opDiv };
    if (ops[op]) return ops[op];
    return code.toLowerCase().replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
};

// Member-weighted average accuracy across all groups (the "platform" accuracy).
export const overallAvgAccuracy = (groups) => {
    let weighted = 0;
    let denom = 0;
    for (const g of groups) {
        if (g.avgAccuracy != null) {
            weighted += g.avgAccuracy * g.memberCount;
            denom += g.memberCount;
        }
    }
    return denom > 0 ? weighted / denom : null;
};

// The group with the most students (for the headline stat). Null-safe.
export const largestGroup = (groups) =>
    groups.reduce((max, g) => (!max || g.memberCount > max.memberCount ? g : max), null);

// Aggregates each focus area across clusters, weighted by how many students sit
// in a cluster that struggles with it → "students affected", sorted desc.
export const aggregateErrors = (groups, t) => {
    const totals = new Map();
    for (const g of groups) {
        for (const code of g.topErrors || []) {
            totals.set(code, (totals.get(code) || 0) + (g.memberCount || 0));
        }
    }
    return [...totals.entries()]
        .map(([code, count]) => ({ code, name: humanizeError(code, t), count }))
        .sort((a, b) => b.count - a.count);
};

// Shared recharts tooltip styling so every chart's tooltip matches the card system.
export const tooltipContentStyle = {
    borderRadius: 10,
    border: "1px solid #ececf1",
    boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
    fontSize: 13,
    padding: "8px 12px",
};
export const tooltipItemStyle = { color: "#2b2b35" };
export const tooltipLabelStyle = { color: COLORS.muted, fontWeight: 600, marginBottom: 2 };
