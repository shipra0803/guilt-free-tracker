import { useEffect, useState } from "react";
import { api } from "../api";
import { formatMoney, formatShortDate } from "../format";

// The selectable date-range presets, in display order.
const RANGES = [
  { key: "WEEK", label: "Past week" },
  { key: "THIRTY_DAYS", label: "Past 30 days" },
  { key: "THIS_MONTH", label: "This month" },
  { key: "LAST_MONTH", label: "Last month" },
  { key: "YEAR", label: "Past year" },
];

// Formats a "YYYY-MM" string as a short month name, e.g. "Jul".
function monthLabel(yearMonth) {
  const [year, month] = yearMonth.split("-").map(Number);
  return new Date(year, month - 1, 1).toLocaleDateString("en-US", { month: "short" });
}

/** A point's label is either "YYYY-MM-DD" (daily ranges) or "YYYY-MM" (YEAR). */
function pointLabel(label) {
  return label.length === 7 ? monthLabel(label) : formatShortDate(label);
}

/**
 * Range-selectable spending history: a bar chart of actual logged spending for the
 * selected window (week/30 days/this month/last month/past year), plus net income vs.
 * total money out for that same window. Fetches independently of the rest of the app so
 * switching ranges doesn't touch the global summary/expenses state.
 */
export default function History() {
  const [range, setRange] = useState("THIS_MONTH");
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Fetches history data whenever the selected range changes.
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .getHistory(range)
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        console.error(err);
        if (!cancelled) setError("Couldn't load history.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [range]);

  // Range picker tabs plus loading/error/content states.
  return (
    <section className="card">
      <h2 className="card-title">Spending history</h2>

      <div className="range-picker" role="group" aria-label="Date range">
        {RANGES.map((r) => (
          <button
            key={r.key}
            type="button"
            aria-pressed={range === r.key}
            className={`range-picker-item ${range === r.key ? "is-active" : ""}`}
            onClick={() => setRange(r.key)}
          >
            {r.label}
          </button>
        ))}
      </div>

      {/* Keep the previous range on screen (dimmed) while the next one loads, so nothing jumps. */}
      <div className={loading ? "is-loading" : ""} aria-busy={loading}>
        {error && <p className="error-text" role="alert">{error}</p>}
        {!error && data && <HistoryContent data={data} />}
        {!error && !data && <div className="skeleton skeleton-chart" />}
      </div>
    </section>
  );
}

// Renders the bar chart and range totals once data has loaded.
function HistoryContent({ data }) {
  const { points, netIncome, fixedTotal, totalSpent, totalOut, difference, estimated } = data;
  const maxSpent = Math.max(...points.map((p) => Number(p.spent)), 1);
  const isPositive = Number(difference) >= 0;

  // Cap visible x-axis labels to roughly 7 so a 30-day view doesn't turn into an
  // unreadable wall of text; always keep the very first and last labels.
  const labelStep = Math.max(1, Math.ceil(points.length / 7));

  // Screen-reader summary standing in for the bars (role="img" hides them from assistive tech).
  const peak = points.reduce((a, b) => (Number(b.spent) > Number(a.spent) ? b : a), points[0]);
  const chartLabel =
    `Spending from ${pointLabel(points[0].label)} to ${pointLabel(points.at(-1).label)}: ` +
    `${formatMoney(totalSpent)} total` +
    (Number(peak.spent) > 0 ? `, highest ${formatMoney(peak.spent)} on ${pointLabel(peak.label)}.` : ".");

  return (
    <>
      {/* Bar chart - one bar per point, height scaled to the largest day/month. */}
      <div className="spending-chart" role="img" aria-label={chartLabel}>
        {points.map((point, index) => {
          const heightPct = (Number(point.spent) / maxSpent) * 100;
          const showLabel = index % labelStep === 0 || index === points.length - 1;
          return (
            <div key={point.label} className="spending-chart-column">
              <div className="spending-chart-bar-track">
                <div
                  className="spending-chart-bar"
                  style={{ height: `${heightPct}%` }}
                  title={`${pointLabel(point.label)}: ${formatMoney(point.spent)}`}
                />
              </div>
              <span className="spending-chart-label">{showLabel ? pointLabel(point.label) : ""}</span>
            </div>
          );
        })}
      </div>

      {Number(totalSpent) === 0 && (
        <p className="empty-state">No expenses logged in this range yet.</p>
      )}

      {/* Range totals grid: income, fixed, spent, total out, and the difference. */}
      <div className="history-summary-grid">
        {[
          ["Net income", formatMoney(netIncome)],
          ["Fixed", formatMoney(fixedTotal)],
          ["Spent", formatMoney(totalSpent)],
          ["Total out", formatMoney(totalOut)],
          ["Difference", `${isPositive ? "+" : "−"}${formatMoney(Math.abs(difference))}`, isPositive ? "" : "is-negative-text"],
        ].map(([label, value, extraClass = ""]) => (
          <div key={label} className="history-summary-item">
            <span className="history-summary-label">{label}</span>
            <span className={`history-summary-value ${extraClass}`}>{value}</span>
          </div>
        ))}
      </div>

      {estimated && (
        <p className="budget-setup-hint">
          Net income and fixed costs for this range are estimated - they're prorated
          across the calendar months this window touches, since paychecks and bills
          don't arrive daily.
        </p>
      )}
    </>
  );
}
