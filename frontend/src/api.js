// Thin wrapper around the backend REST API. Base URL comes from an env var so the
// same build works against localhost in dev and a deployed backend in production
// (set VITE_API_BASE_URL when deploying the frontend - see the root README).
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

// Shared fetch helper - adds the base URL/JSON header, throws on non-OK responses,
// and returns null for 204 responses instead of trying to parse an empty body.
async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!response.ok) {
    const body = await response.text().catch(() => "");
    throw new Error(`${options.method || "GET"} ${path} failed (${response.status}): ${body}`);
  }

  if (response.status === 204) return null;
  return response.json();
}

export const api = {
  // Everything the Home and Settings pages need: net income, fixed total, extra
  // savings, and the flexible category list with spent/remaining per category.
  getSummary: () => request("/api/summary"),

  // Spending-over-time chart + range totals for the History page. range is one of
  // WEEK, THIRTY_DAYS, THIS_MONTH, LAST_MONTH, YEAR.
  getHistory: (range = "THIS_MONTH") => request(`/api/history?range=${range}`),

  // Expenses - log, list, delete.
  getExpenses: () => request("/api/expenses"),
  addExpense: (expense) =>
    request("/api/expenses", { method: "POST", body: JSON.stringify(expense) }),
  deleteExpense: (id) => request(`/api/expenses/${id}`, { method: "DELETE" }),

  // Income profile - read, live estimate, save.
  getIncome: () => request("/api/income"),
  estimateIncome: (yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate) =>
    request("/api/income/estimate", {
      method: "POST",
      body: JSON.stringify({ yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate }),
    }),
  saveIncome: (yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate) =>
    request("/api/income", {
      method: "PUT",
      body: JSON.stringify({ yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate }),
    }),

  // Fixed expenses - list, add, delete.
  getFixedExpenses: () => request("/api/fixed-expenses"),
  addFixedExpense: (name, monthlyAmount) =>
    request("/api/fixed-expenses", { method: "POST", body: JSON.stringify({ name, monthlyAmount }) }),
  deleteFixedExpense: (id) => request(`/api/fixed-expenses/${id}`, { method: "DELETE" }),

  // Flexible categories - list, add, update, delete.
  getFlexibleCategories: () => request("/api/flexible-categories"),
  addFlexibleCategory: (name, monthlyBudget) =>
    request("/api/flexible-categories", { method: "POST", body: JSON.stringify({ name, monthlyBudget }) }),
  updateFlexibleCategory: (id, name, monthlyBudget) =>
    request(`/api/flexible-categories/${id}`, { method: "PUT", body: JSON.stringify({ name, monthlyBudget }) }),
  deleteFlexibleCategory: (id) => request(`/api/flexible-categories/${id}`, { method: "DELETE" }),
};
