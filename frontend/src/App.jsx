import { useCallback, useEffect, useState } from "react";
import { api, auth } from "./api";
import Login from "./components/Login";
import FlexibleRemainingHero from "./components/FlexibleRemainingHero";
import ExpenseForm from "./components/ExpenseForm";
import ExpenseList from "./components/ExpenseList";
import History from "./components/History";
import Settings from "./components/Settings";
import "./App.css";

// The three top-level tabs, in display order. No router - just view state.
const TABS = [
  { key: "home", label: "Home" },
  { key: "history", label: "History" },
  { key: "settings", label: "Settings" },
];

// Root component - owns all shared state and switches between the three tabs.
export default function App() {
  // Login state: whether credentials are stored, and why we're back on the login page.
  const [loggedIn, setLoggedIn] = useState(auth.isLoggedIn());
  const [loginError, setLoginError] = useState(null);

  // Shared app state, fetched from the backend and passed down to each tab.
  const [activeTab, setActiveTab] = useState("home");
  const [summary, setSummary] = useState(null);
  const [expenses, setExpenses] = useState([]);
  const [incomeProfile, setIncomeProfile] = useState(null);
  const [fixedExpenses, setFixedExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // Reloads every piece of shared state from the backend in parallel.
  const refresh = useCallback(async () => {
    const [summaryData, expenseData, incomeData, fixedExpenseData] = await Promise.all([
      api.getSummary(),
      api.getExpenses(),
      api.getIncome(),
      api.getFixedExpenses(),
    ]);
    setSummary(summaryData);
    setExpenses(expenseData);
    setIncomeProfile(incomeData);
    setFixedExpenses(fixedExpenseData);
  }, []);

  // Forget the stored credentials and show the login page (optionally with a reason).
  const logOut = useCallback((reason = null) => {
    auth.logOut();
    setLoginError(reason);
    setLoggedIn(false);
  }, []);

  // Initial load (and retry after a failed one). A 401 means the login was wrong.
  const load = useCallback(() => {
    setLoading(true);
    setLoadError(null);
    refresh()
      .catch((err) => {
        console.error(err);
        if (err.status === 401) logOut("That username and password didn't work.");
        else setLoadError("Couldn't load your budget. Check that the server is running, then try again.");
      })
      .finally(() => setLoading(false));
  }, [refresh, logOut]);

  useEffect(() => {
    if (loggedIn) load();
  }, [loggedIn, load]);

  // Wraps an API call so shared state is refreshed after it succeeds. If the server stops
  // accepting our login mid-session (e.g. the password changed), go back to the login page.
  const withRefresh = (fn) => async (...args) => {
    try {
      await fn(...args);
      await refresh();
    } catch (err) {
      if (err.status === 401) logOut("Your session ended. Please log in again.");
      throw err;
    }
  };

  if (!loggedIn) {
    return (
      <Login
        error={loginError}
        onLogin={(username, password) => {
          auth.logIn(username, password);
          setLoggedIn(true);
        }}
      />
    );
  }

  // Loading state - shown until the initial refresh() resolves.
  // Skeleton shaped like the Home page.
  if (loading) {
    return (
      <div className="app-shell" aria-busy="true" aria-label="Loading your budget">
        <div className="skeleton skeleton-header" />
        <div className="skeleton skeleton-hero" />
        <div className="skeleton skeleton-card" />
        <div className="skeleton skeleton-card" />
      </div>
    );
  }

  // Error state - shown if the initial refresh() fails.
  if (loadError) {
    return (
      <div className="app-shell app-shell-center">
        <p className="error-text" role="alert">{loadError}</p>
        <button type="button" className="btn-primary" onClick={load}>
          Try again
        </button>
      </div>
    );
  }

  // Flexible categories (with spent/remaining) come from the summary payload.
  const flexibleCategories = summary?.categories ?? [];

  // Main layout: header with nav, then whichever tab is active.
  return (
    <div className="app-shell">
      <header className="app-header">
        <h1 className="wordmark">guilt-free.</h1>
        <div className="header-actions">
          <nav
            className="tab-nav"
            aria-label="Pages"
            style={{ "--active": TABS.findIndex((tab) => tab.key === activeTab) }}
          >
            {TABS.map((tab) => (
              <button
                key={tab.key}
                type="button"
                aria-current={activeTab === tab.key ? "page" : undefined}
                className={`tab-nav-item ${activeTab === tab.key ? "is-active" : ""}`}
                onClick={() => setActiveTab(tab.key)}
              >
                {tab.label}
              </button>
            ))}
          </nav>
          <button type="button" className="btn-logout" onClick={() => logOut()}>
            Log out
          </button>
        </div>
      </header>

      <main className="app-main">
        {/* Home: the flexible-remaining number, the expense form, and recent transactions. */}
        {activeTab === "home" && (
          <>
            <FlexibleRemainingHero summary={summary} />
            <section className="card">
              <ExpenseForm categories={flexibleCategories} onAdd={withRefresh(api.addExpense)} />
            </section>
            <section className="card">
              <h2 className="card-title">Recent transactions</h2>
              <ExpenseList expenses={expenses} onDelete={withRefresh(api.deleteExpense)} />
            </section>
          </>
        )}

        {activeTab === "history" && <History />}

        {activeTab === "settings" && (
          <Settings
            summary={summary}
            incomeProfile={incomeProfile}
            fixedExpenses={fixedExpenses}
            flexibleCategories={flexibleCategories}
            onEstimateIncome={api.estimateIncome}
            onSaveIncome={withRefresh(api.saveIncome)}
            onAddFixedExpense={withRefresh(api.addFixedExpense)}
            onUpdateFixedExpense={withRefresh(api.updateFixedExpense)}
            onDeleteFixedExpense={withRefresh(api.deleteFixedExpense)}
            onAddFlexibleCategory={withRefresh(api.addFlexibleCategory)}
            onUpdateFlexibleCategory={withRefresh(api.updateFlexibleCategory)}
            onDeleteFlexibleCategory={withRefresh(api.deleteFlexibleCategory)}
          />
        )}
      </main>
    </div>
  );
}
