/* Shared primitives intentionally export both components and their visual constants. */
/* eslint-disable react-refresh/only-export-components */
import { useEffect } from 'react';
import { motion, animate, useMotionValue, useTransform } from 'framer-motion';
import { RadialBarChart, RadialBar, PolarAngleAxis, ResponsiveContainer } from 'recharts';

// Shared presentational primitives for the dashboard, built on recharts (charts)
// and framer-motion (animations).

// MathGalaxy palette — purple/blue only (no green/red) for the themed visuals.
export const COLORS = {
    primary: '#7C4DFF',
    blue:    '#5B8CFF',
    purple:  '#7c3aed',
    muted:   'var(--mg-tm)',
    track:   'rgba(124, 77, 255, .14)',
};

// Gauge/bar colour — a calm purple→blue, never green/red. Stronger purple for high rates.
export const rateColor = (rate) => ((rate ?? 0) >= 60 ? COLORS.primary : COLORS.blue);

// framer-motion variants: the container staggers its children into view.
export const containerVariants = {
    hidden: {},
    show: { transition: { staggerChildren: 0.09, delayChildren: 0.05 } },
};
export const itemVariants = {
    hidden: { opacity: 0, y: 18 },
    show: { opacity: 1, y: 0, transition: { duration: 0.45, ease: [0.22, 1, 0.36, 1] } },
};

// Animated card. Fades/slides in as part of the staggered container and lifts on hover.
export const Card = ({ title, action, children, style }) => (
    <motion.section
        variants={itemVariants}
        whileHover={{ y: -3, boxShadow: '0 10px 28px rgba(0,0,0,0.10)' }}
        transition={{ type: 'spring', stiffness: 300, damping: 24 }}
        style={{ ...cardStyle, ...style }}
    >
        {(title || action) && (
            <header style={cardHeader}>
                {title && <h3 style={cardTitle}>{title}</h3>}
                {action}
            </header>
        )}
        {children}
    </motion.section>
);

// A number that counts up from 0 when it mounts/updates.
export const AnimatedNumber = ({ value, format = (v) => Math.round(v).toLocaleString() }) => {
    const mv = useMotionValue(0);
    const text = useTransform(mv, (v) => format(v));
    useEffect(() => {
        const controls = animate(mv, value || 0, { duration: 1.1, ease: 'easeOut' });
        return controls.stop;
    }, [value, mv]);
    return <motion.span>{text}</motion.span>;
};

// Circular gauge (recharts radial bar) used for the headline success rate.
export const GaugeChart = ({ percent, color = COLORS.primary, size = 160, label }) => {
    const value = Math.min(100, Math.max(0, percent || 0));
    const data = [{ name: 'rate', value }];
    return (
        <div style={{ position: 'relative', width: size, height: size }}>
            <ResponsiveContainer width="100%" height="100%">
                <RadialBarChart innerRadius="72%" outerRadius="100%" data={data} startAngle={90} endAngle={-270}>
                    <PolarAngleAxis type="number" domain={[0, 100]} tick={false} />
                    <RadialBar dataKey="value" cornerRadius={20} fill={color} background={{ fill: COLORS.track }} />
                </RadialBarChart>
            </ResponsiveContainer>
            <div style={gaugeCenter}>
                <span style={{ fontSize: 30, fontWeight: 800, color: 'var(--mg-ts)' }}>
                    <AnimatedNumber value={value} format={(v) => `${Math.round(v)}%`} />
                </span>
                {label && <span style={{ fontSize: 11, color: 'var(--mg-tm)' }}>{label}</span>}
            </div>
        </div>
    );
};

// Maps an error-pattern code into a short, kid-friendly label for display.
// Localized: pulls the display text from the dashboard strings dictionary `t`.
export const prettifyError = (code, t) => {
    if (!code) return '';
    const map = {
        CONFUSED_SUB_WITH_ADD:   t.errConfusedSubAdd,
        MINOR_CALCULATION_ERROR: t.errMinorCalc,
        EMPTY_ANSWER:            t.errEmptyAnswer,
        INVALID_FORMAT_ERROR:    t.errInvalidFormat,
    };
    if (map[code]) return map[code];
    // Operation codes (e.g. GENERAL_ERROR_MULT → "Times tables") — plain words, no symbols.
    const op = code.replace(/^GENERAL_ERROR_/, '').toLowerCase();
    const opMap = {
        mult: t.opMult,
        add:  t.opAdd,
        sub:  t.opSub,
        div:  t.opDiv,
    };
    return opMap[op] || op.replace(/_/g, ' ');
};

// Turns the raw cluster label (e.g. "Developing - mainly MINOR_CALCULATION_ERROR")
// into a fun, kid-friendly group name. The detailed focus areas show as chips below.
// Localized via the dashboard strings dictionary `t`.
export const prettifyClusterLabel = (label, t) => {
    if (!label) return t.groupDefault;
    const base = label.split(' - mainly ')[0].trim().toLowerCase();
    if (base.includes('high'))       return t.groupMathStar;
    if (base.includes('needs'))      return t.groupJustStarting;
    if (base.includes('developing')) return t.groupRisingStar;
    return label.split(' - mainly ')[0]; // unknown label → show the base part only
};

export const severityColor = (severity) => (
    severity === 'high' ? COLORS.purple : severity === 'medium' ? COLORS.primary : COLORS.blue
);

// ── styles ──────────────────────────────────────────────────────────────────
const cardStyle = {
    border: '1px solid var(--mg-bd)', borderRadius: '20px', padding: '22px',
    background: 'var(--mg-card)', boxShadow: 'var(--mg-cardsh)',
};
const cardHeader = { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' };
const cardTitle  = { margin: 0, fontSize: '16px', fontWeight: 800, color: 'var(--mg-ts)' };
const gaugeCenter = {
    position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column',
    alignItems: 'center', justifyContent: 'center', pointerEvents: 'none',
};
