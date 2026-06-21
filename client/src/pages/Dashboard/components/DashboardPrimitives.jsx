import { useEffect } from 'react';
import { motion, animate, useMotionValue, useTransform } from 'framer-motion';
import { RadialBarChart, RadialBar, PolarAngleAxis, ResponsiveContainer } from 'recharts';

// Shared presentational primitives for the dashboard, built on recharts (charts)
// and framer-motion (animations).

export const COLORS = {
    primary: '#007bff',
    success: '#28a745',
    error:   '#dc3545',
    warning: '#fd7e14',
    purple:  '#7c3aed',
    muted:   '#888',
    track:   '#eef1f5',
};

// Performance → colour scale shared by the bars/gauges.
export const rateColor = (rate) => (rate >= 70 ? COLORS.success : rate >= 40 ? COLORS.warning : COLORS.error);

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
                <span style={{ fontSize: 30, fontWeight: 'bold', color: '#222' }}>
                    <AnimatedNumber value={value} format={(v) => `${Math.round(v)}%`} />
                </span>
                {label && <span style={{ fontSize: 11, color: COLORS.muted }}>{label}</span>}
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
    severity === 'high' ? COLORS.error : severity === 'medium' ? COLORS.warning : COLORS.primary
);

// ── styles ──────────────────────────────────────────────────────────────────
const cardStyle = {
    border: '1px solid #ececf1', borderRadius: '16px', padding: '20px',
    backgroundColor: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
};
const cardHeader = { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' };
const cardTitle  = { margin: 0, fontSize: '16px', color: '#2b2b35' };
const gaugeCenter = {
    position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column',
    alignItems: 'center', justifyContent: 'center', pointerEvents: 'none',
};
