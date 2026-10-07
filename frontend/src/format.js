// Small formatting helpers shared across components (currency and date display).
const money = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
});

// Formats a number as USD currency, treating null/undefined as zero.
export function formatMoney(value) {
  return money.format(Number(value ?? 0));
}

/** Formats a "YYYY-MM-DD" date string without timezone drift, e.g. "Jul 2". */
export function formatShortDate(isoDate) {
  const [year, month, day] = isoDate.split("-").map(Number);
  const date = new Date(year, month - 1, day);
  return date.toLocaleDateString("en-US", { month: "short", day: "numeric" });
}

// Returns today's local date as "YYYY-MM-DD" (en-CA formats that way), used to default date inputs.
export function todayIso() {
  return new Date().toLocaleDateString("en-CA");
}
