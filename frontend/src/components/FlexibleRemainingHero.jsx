import { formatMoney } from "../format";

/**
 * The star of the Home page: how much of your Flexible budget is left this month,
 * across every category combined. Replaces the old daily "guilt-free" number - this
 * app now answers "what's left to spend on myself" at the month level, not the day.
 */
export default function FlexibleRemainingHero({ summary }) {
  if (!summary) return null;

  // Pulls the flexible totals out of the summary payload.
  const remaining = Number(summary.flexibleRemainingThisMonth ?? 0);
  const allocated = Number(summary.flexibleAllocated ?? 0);
  const spent = Number(summary.flexibleSpentThisMonth ?? 0);
  const isNegative = remaining < 0;

  // Big hero number, a warning if over budget, and the spent/allocated hint line.
  return (
    <div className="hero-number">
      <p className="hero-eyebrow">Flexible spending left this month</p>
      <p className={`hero-amount ${isNegative ? "is-negative" : ""}`}>
        {isNegative ? `–${formatMoney(Math.abs(remaining))}` : formatMoney(remaining)}
      </p>
      {isNegative && (
        <p className="hero-negative-note">
          You've spent past your flexible budget for this month — check Settings to see which category.
        </p>
      )}
      <p className="hero-hint">
        <strong>{formatMoney(spent)}</strong> spent of <strong>{formatMoney(allocated)}</strong> allocated
        across your flexible categories.
      </p>
    </div>
  );
}
