// Thin wrapper around the backend REST API. Base URL comes from an env var so the
// same build works against localhost in dev and a deployed backend in production
// (set VITE_API_BASE_URL when deploying the frontend - see the root README).
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

// Login: the backend checks a username/password (HTTP Basic) on every request. We keep the
// encoded "username:password" in sessionStorage, so closing the tab logs you out.
const AUTH_KEY = "auth";

export const auth = {
  isLoggedIn: () => Boolean(sessionStorage.getItem(AUTH_KEY)),
  // TextEncoder + btoa encodes the credentials as UTF-8, so non-ASCII passwords work too.
  logIn: (username, password) =>
    sessionStorage.setItem(
      AUTH_KEY,
      btoa(String.fromCharCode(...new TextEncoder().encode(`${username}:${password}`))),
    ),
  logOut: () => sessionStorage.removeItem(AUTH_KEY),
};

// Shared fetch helper - adds the base URL, JSON and login headers, throws on non-OK
// responses (with .status, so callers can spot a 401), and returns null for 204 responses.
async function request(path, options = {}) {
  const credentials = sessionStorage.getItem(AUTH_KEY);
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(credentials && { Authorization: `Basic ${credentials}` }),
    },
    ...options,
  });

  if (!response.ok) {
    const body = await response.text().catch(() => "");
    const error = new Error(`${options.method || "GET"} ${path} failed (${response.status}): ${body}`);
    error.status = response.status;
    throw error;
  }

  if (response.status === 204) return null;
  return response.json();
}

export const api = {
  // First-run sign-up: { open } says whether an account can still be created.
  getSignupStatus: () => request("/api/signup"),
  signUp: (username, password) =>
    request("/api/signup", { method: "POST", body: JSON.stringify({ username, password }) }),

  // Everything the Home and Settings pages need: net income, fixed total, extra
  // savings, and the flexible category list with spent/remaining per category.
  getSummary: () => request("/api/summary"),

  // Spending-over-time chart + range totals for the History page. range is one of
  // WEEK, THIRTY_DAYS, THIS_MONTH, LAST_MONTH, YEAR.
  getHistory: (range) => request(`/api/history?range=${range}`),

  // Expenses - log, list, delete.
  getExpenses: () => request("/api/expenses"),
  addExpense: (expense) =>
    request("/api/expenses", { method: "POST", body: JSON.stringify(expense) }),
  deleteExpense: (id) => request(`/api/expenses/${id}`, { method: "DELETE" }),

  // Income profile - read, live estimate, save. income is
  // { yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate }.
  getIncome: () => request("/api/income"),
  estimateIncome: (income) => request("/api/income/estimate", { method: "POST", body: JSON.stringify(income) }),
  saveIncome: (income) => request("/api/income", { method: "PUT", body: JSON.stringify(income) }),

  // Fixed expenses - list, add, update, delete.
  getFixedExpenses: () => request("/api/fixed-expenses"),
  addFixedExpense: (name, monthlyAmount) =>
    request("/api/fixed-expenses", { method: "POST", body: JSON.stringify({ name, monthlyAmount }) }),
  updateFixedExpense: (id, name, monthlyAmount) =>
    request(`/api/fixed-expenses/${id}`, { method: "PUT", body: JSON.stringify({ name, monthlyAmount }) }),
  deleteFixedExpense: (id) => request(`/api/fixed-expenses/${id}`, { method: "DELETE" }),

  // Flexible categories - add, update, delete (listing comes from getSummary).
  addFlexibleCategory: (name, monthlyBudget) =>
    request("/api/flexible-categories", { method: "POST", body: JSON.stringify({ name, monthlyBudget }) }),
  updateFlexibleCategory: (id, name, monthlyBudget) =>
    request(`/api/flexible-categories/${id}`, { method: "PUT", body: JSON.stringify({ name, monthlyBudget }) }),
  deleteFlexibleCategory: (id) => request(`/api/flexible-categories/${id}`, { method: "DELETE" }),
};
