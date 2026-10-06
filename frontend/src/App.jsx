import { useCallback, useEffect, useState } from "react";
import { api } from "./api";
import Nav from "./components/Nav";
import Home from "./components/Home";
import History from "./components/History";
import Settings from "./components/Settings";
import "./App.css";

// Root component - owns all shared state and switches between the three tabs.
export default function App() {
  // Shared app state, fetched from the backend and passed down to each tab.
  const [activeTab, setActiveTab] = useState("home");
  const [summary, setSummary] = useState(null);
  const [expenses, setExpenses] = useState([]);
  const [incomeProfile, setIncomeProfile] = useState(null);
  const [fixedExpenses, setFixedExpenses] = useState([]);
  const [flexibleCategories, setFlexibleCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // Reloads every piece of shared state from the backend in parallel.
  const refresh = useCallback(async () => {
    const [summaryData, expenseData, incomeData, fixedExpenseData, flexibleCategoryData] =
      await Promise.all([
        api.getSummary(),
        api.getExpenses(),
        api.getIncome(),
        api.getFixedExpenses(),
        api.getFlexibleCategories(),
      ]);
    setSummary(summaryData);
    setExpenses(expenseData);
    setIncomeProfile(incomeData);
    setFixedExpenses(fixedExpenseData);
    setFlexibleCategories(flexibleCategoryData);
  }, []);

  // Initial load on mount.
  useEffect(() => {
    refresh()
      .catch((err) => {
        console.error(err);
        setLoadError("Couldn't reach the backend. Make sure the API is running.");
      })
      .finally(() => setLoading(false));
  }, [refresh]);

  // Expense handlers - call the API, then refresh shared state.
  async function handleAddExpense(expense) {
    await api.addExpense(expense);
    await refresh();
  }

  async function handleDeleteExpense(id) {
    await api.deleteExpense(id);
    await refresh();
  }

  // Income handlers - estimate doesn't persist, save does.
  async function handleEstimateIncome(yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate) {
    return api.estimateIncome(yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate);
  }

  async function handleSaveIncome(yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate) {
    await api.saveIncome(yearlySalary, stateTaxRatePercent, payFrequency, anchorPayDate);
    await refresh();
  }

  // Fixed expense handlers.
  async function handleAddFixedExpense(name, monthlyAmount) {
    await api.addFixedExpense(name, monthlyAmount);
    await refresh();
  }

  async function handleDeleteFixedExpense(id) {
    await api.deleteFixedExpense(id);
    await refresh();
  }

  // Flexible category handlers.
  async function handleAddFlexibleCategory(name, monthlyBudget) {
    await api.addFlexibleCategory(name, monthlyBudget);
    await refresh();
  }

  async function handleUpdateFlexibleCategory(id, name, monthlyBudget) {
    await api.updateFlexibleCategory(id, name, monthlyBudget);
    await refresh();
  }

  async function handleDeleteFlexibleCategory(id) {
    await api.deleteFlexibleCategory(id);
    await refresh();
  }

  // Loading state - shown until the initial refresh() resolves.
  if (loading) {
    return (
      <div className="app-shell app-shell-center">
        <p className="loading-text">Loading your budget…</p>
      </div>
    );
  }

  // Error state - shown if the initial refresh() fails.
  if (loadError) {
    return (
      <div className="app-shell app-shell-center">
        <p className="error-text">{loadError}</p>
      </div>
    );
  }

  // Main layout: header with nav, then whichever tab is active.
  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="wordmark">guilt-free.</div>
        <Nav active={activeTab} onChange={setActiveTab} />
      </header>

      <main className="app-main">
        {activeTab === "home" && (
          <Home
            summary={summary}
            expenses={expenses}
            flexibleCategories={flexibleCategories}
            onAddExpense={handleAddExpense}
            onDeleteExpense={handleDeleteExpense}
          />
        )}

        {activeTab === "history" && <History />}

        {activeTab === "settings" && (
          <Settings
            summary={summary}
            incomeProfile={incomeProfile}
            fixedExpenses={fixedExpenses}
            flexibleCategories={flexibleCategories}
            onEstimateIncome={handleEstimateIncome}
            onSaveIncome={handleSaveIncome}
            onAddFixedExpense={handleAddFixedExpense}
            onDeleteFixedExpense={handleDeleteFixedExpense}
            onAddFlexibleCategory={handleAddFlexibleCategory}
            onUpdateFlexibleCategory={handleUpdateFlexibleCategory}
            onDeleteFlexibleCategory={handleDeleteFlexibleCategory}
          />
        )}
      </main>
    </div>
  );
}
