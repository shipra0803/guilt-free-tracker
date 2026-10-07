import { formatMoney } from "../format";

/**
 * The star of the Home page: how much of your Flexible budget is left this month,
 * across every category combined, plus what that works out to per remaining day.
 */
export default function FlexibleRemainingHero({ summary }) {
  if (!summary) return null;

  // Pulls the flexible totals out of the summary payload.
  const remaining = Number(summary.flexibleRemainingThisMonth ?? 0);
  const allocated = Number(summary.flexibleAllocated ?? 0);
  const spent = Number(summary.flexibleSpentThisMonth ?? 0);
  const isNegative = remaining < 0;

  // Days left in this month, counting today. Day 0 of next month = last day of this month.
  const today = new Date();
  const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0).getDate();
  const daysLeft = lastDay - today.getDate() + 1;

  // Big hero number, a warning if over budget, and the spent/allocated hint line.
  return (
    <div className="hero-number">
      <p className="hero-eyebrow">Flexible spending left this month</p>
      {/* key: re-mount (and replay the "pop" animation) whenever the amount changes. */}
      <p key={remaining} className={`hero-amount ${isNegative ? "is-negative" : ""}`}>
        {isNegative ? `−${formatMoney(Math.abs(remaining))}` : formatMoney(remaining)}
      </p>
      {isNegative ? (
        <p className="hero-negative-note">
          You've spent past your flexible budget for this month. Check Settings to see which category.
        </p>
      ) : (
        <p className="hero-daily">
          About <strong>{formatMoney(remaining / daysLeft)}</strong> a day for the {daysLeft}{" "}
          {daysLeft === 1 ? "day" : "days"} left this month.
        </p>
      )}
      <p className="hero-hint">
        <strong>{formatMoney(spent)}</strong> spent of <strong>{formatMoney(allocated)}</strong> allocated
        across your flexible categories.
      </p>
    </div>
  );
}
