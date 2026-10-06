import { useEffect, useState } from "react";
import { formatMoney, todayIso } from "../format";

// Dropdown options for pay frequency.
const PAY_FREQUENCIES = [
  { value: "MONTHLY", label: "Monthly" },
  { value: "SEMI_MONTHLY", label: "Semi-monthly (twice a month)" },
  { value: "BIWEEKLY", label: "Biweekly (every 2 weeks)" },
  { value: "WEEKLY", label: "Weekly" },
];

// Frequencies that need an anchor pay date to compute paycheck counts.
const NEEDS_ANCHOR_DATE = new Set(["BIWEEKLY", "WEEKLY"]);

// Usual (non-bonus) paycheck count per month, per frequency - used only to decide
// whether to show the "bonus paycheck" callout.
const USUAL_PAYCHECKS_PER_MONTH = { MONTHLY: 1, SEMI_MONTHLY: 2, BIWEEKLY: 2, WEEKLY: 4 };

/**
 * Where the budget actually gets built: salary + taxes -> net income, fixed
 * expenses, and flexible category budgets. The Home page just displays the result
 * of what's set up here - this is the only place any of these three numbers are
 * edited.
 */
export default function Settings({
  summary,
  incomeProfile,
  fixedExpenses,
  flexibleCategories,
  onEstimateIncome,
  onSaveIncome,
  onAddFixedExpense,
  onDeleteFixedExpense,
  onAddFlexibleCategory,
  onUpdateFlexibleCategory,
  onDeleteFlexibleCategory,
}) {
  // Income & taxes form state.
  const [salary, setSalary] = useState(incomeProfile?.yearlySalary || "");
  const [stateRate, setStateRate] = useState(incomeProfile?.stateTaxRatePercent ?? 0);
  const [payFrequency, setPayFrequency] = useState(incomeProfile?.payFrequency || "MONTHLY");
  const [anchorPayDate, setAnchorPayDate] = useState(incomeProfile?.anchorPayDate || todayIso());
  const [breakdown, setBreakdown] = useState(null);
  const [estimating, setEstimating] = useState(false);
  const [savingIncome, setSavingIncome] = useState(false);

  // Add-fixed-expense form state.
  const [fixedName, setFixedName] = useState("");
  const [fixedAmount, setFixedAmount] = useState("");
  const [addingFixed, setAddingFixed] = useState(false);

  // Add-flexible-category form state.
  const [categoryName, setCategoryName] = useState("");
  const [categoryBudget, setCategoryBudget] = useState("");
  const [addingCategory, setAddingCategory] = useState(false);
  const [categoryError, setCategoryError] = useState(null);

  // Edit-flexible-category (inline) form state.
  const [editingCategoryId, setEditingCategoryId] = useState(null);
  const [editName, setEditName] = useState("");
  const [editBudget, setEditBudget] = useState("");
  const [savingEdit, setSavingEdit] = useState(false);

  // Live preview: re-estimate whenever salary or state rate changes, debounced so
  // we're not hitting the API on every keystroke.
  useEffect(() => {
    const parsedSalary = Number(salary);
    if (!parsedSalary || parsedSalary <= 0) {
      setBreakdown(null);
      return;
    }
    const parsedRate = Number(stateRate) || 0;
    const anchor = NEEDS_ANCHOR_DATE.has(payFrequency) ? anchorPayDate : null;

    setEstimating(true);
    const timeout = setTimeout(() => {
      onEstimateIncome(parsedSalary, parsedRate, payFrequency, anchor)
        .then(setBreakdown)
        .catch((err) => console.error(err))
        .finally(() => setEstimating(false));
    }, 400);

    return () => clearTimeout(timeout);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [salary, stateRate, payFrequency, anchorPayDate]);

  // Sum of all fixed expenses' monthly amounts, for the hint line under that section.
  const fixedTotal = fixedExpenses.reduce((sum, e) => sum + Number(e.monthlyAmount), 0);

  // Persists the income profile using the currently previewed values.
  async function handleSaveIncome() {
    const parsedSalary = Number(salary);
    if (!parsedSalary || parsedSalary <= 0) return;
    const anchor = NEEDS_ANCHOR_DATE.has(payFrequency) ? anchorPayDate : null;
    setSavingIncome(true);
    try {
      await onSaveIncome(parsedSalary, Number(stateRate) || 0, payFrequency, anchor);
    } finally {
      setSavingIncome(false);
    }
  }

  // Adds a new fixed expense and resets the form.
  async function handleAddFixed(event) {
    event.preventDefault();
    const parsedAmount = Number(fixedAmount);
    if (!fixedName.trim() || !parsedAmount || parsedAmount <= 0) return;
    setAddingFixed(true);
    try {
      await onAddFixedExpense(fixedName.trim(), parsedAmount);
      setFixedName("");
      setFixedAmount("");
    } finally {
      setAddingFixed(false);
    }
  }

  // Adds a new flexible category and resets the form.
  async function handleAddCategory(event) {
    event.preventDefault();
    const parsedBudget = Number(categoryBudget);
    if (!categoryName.trim() || !parsedBudget || parsedBudget <= 0) return;
    setAddingCategory(true);
    setCategoryError(null);
    try {
      await onAddFlexibleCategory(categoryName.trim(), parsedBudget);
      setCategoryName("");
      setCategoryBudget("");
    } finally {
      setAddingCategory(false);
    }
  }

  // Deletes a category, surfacing the backend's 409 message if it still has expenses.
  async function handleDeleteCategory(id) {
    setCategoryError(null);
    try {
      await onDeleteFlexibleCategory(id);
    } catch (err) {
      setCategoryError("Can't delete that category - it still has expenses logged against it.");
      console.error(err);
    }
  }

  // Opens the inline edit form for a category, pre-filled with its current values.
  function startEditingCategory(category) {
    setCategoryError(null);
    setEditingCategoryId(category.id);
    setEditName(category.name);
    setEditBudget(String(category.monthlyBudget));
  }

  // Closes the inline edit form without saving.
  function cancelEditingCategory() {
    setEditingCategoryId(null);
  }

  // Saves the inline-edited category.
  async function handleSaveEditCategory(event, id) {
    event.preventDefault();
    const parsedBudget = Number(editBudget);
    if (!editName.trim() || !parsedBudget || parsedBudget <= 0) return;
    setSavingEdit(true);
    setCategoryError(null);
    try {
      await onUpdateFlexibleCategory(id, editName.trim(), parsedBudget);
      setEditingCategoryId(null);
    } catch (err) {
      setCategoryError("Couldn't save that change - try again.");
      console.error(err);
    } finally {
      setSavingEdit(false);
    }
  }

  // Lookup from category id to its spent/remaining breakdown from the summary payload.
  const categoryBreakdownById = new Map((summary?.categories || []).map((c) => [c.id, c]));

  return (
    <>
      {/* Income & taxes: salary/rate/frequency inputs plus the live tax breakdown preview. */}
      <section className="card">
        <h2 className="card-title">Income &amp; taxes</h2>
        <div className="expense-form-row">
          <div className="field field-amount">
            <label htmlFor="yearly-salary">Yearly salary</label>
            <div className="field-amount-input">
              <span aria-hidden="true">$</span>
              <input
                id="yearly-salary"
                type="number"
                min="0"
                step="1000"
                placeholder="65000"
                value={salary}
                onChange={(e) => setSalary(e.target.value)}
              />
            </div>
          </div>
          <div className="field field-amount">
            <label htmlFor="state-rate">State tax rate</label>
            <div className="field-amount-input">
              <input
                id="state-rate"
                type="number"
                min="0"
                step="0.1"
                placeholder="0"
                value={stateRate}
                onChange={(e) => setStateRate(e.target.value)}
              />
              <span aria-hidden="true">%</span>
            </div>
          </div>
        </div>

        <div className="expense-form-row">
          <div className="field field-description">
            <label htmlFor="pay-frequency">Pay frequency</label>
            <select
              id="pay-frequency"
              className="pay-frequency-select"
              value={payFrequency}
              onChange={(e) => setPayFrequency(e.target.value)}
            >
              {PAY_FREQUENCIES.map((f) => (
                <option key={f.value} value={f.value}>
                  {f.label}
                </option>
              ))}
            </select>
          </div>
          {NEEDS_ANCHOR_DATE.has(payFrequency) && (
            <div className="field field-date">
              <label htmlFor="anchor-pay-date">A recent payday</label>
              <input
                id="anchor-pay-date"
                type="date"
                value={anchorPayDate}
                onChange={(e) => setAnchorPayDate(e.target.value)}
              />
            </div>
          )}
        </div>
        <p className="budget-setup-hint">
          {NEEDS_ANCHOR_DATE.has(payFrequency) &&
            " "}
        </p>

        {estimating && <p className="budget-setup-hint">Calculating…</p>}

        {breakdown && !estimating && (
          <div className="tax-breakdown">
            <div className="tax-breakdown-row">
              <span>Gross annual</span>
              <span>{formatMoney(breakdown.grossAnnual)}</span>
            </div>
            <div className="tax-breakdown-row">
              <span>Federal tax</span>
              <span>-{formatMoney(breakdown.federalTax)}</span>
            </div>
            <div className="tax-breakdown-row">
              <span>FICA (Social Security + Medicare)</span>
              <span>-{formatMoney(breakdown.ficaTax)}</span>
            </div>
            <div className="tax-breakdown-row">
              <span>State tax</span>
              <span>-{formatMoney(breakdown.stateTax)}</span>
            </div>
            <div className="tax-breakdown-row">
              <span>Net annual</span>
              <span>{formatMoney(breakdown.netAnnual)}</span>
            </div>
            <div className="tax-breakdown-row">
              <span>Average monthly (net ÷ 12)</span>
              <span>{formatMoney(breakdown.netMonthlyAverage)}</span>
            </div>
            <div className="tax-breakdown-row tax-breakdown-total">
              <span>
                This month ({breakdown.paychecksThisMonth} paycheck{breakdown.paychecksThisMonth === 1 ? "" : "s"})
                {breakdown.paychecksThisMonth > USUAL_PAYCHECKS_PER_MONTH[payFrequency] && " — bonus paycheck!"}
              </span>
              <span>{formatMoney(breakdown.netThisMonth)}</span>
            </div>
          </div>
        )}

        <button type="button" className="btn-primary" onClick={handleSaveIncome} disabled={savingIncome || !breakdown}>
          {savingIncome ? "Saving…" : "Save income"}
        </button>
      </section>

      {/* Fixed expenses: recurring costs list plus the add form. */}
      <section className="card">
        <h2 className="card-title">Fixed expenses</h2>
        <p className="budget-setup-hint">Rent, utilities, car payment — costs you don't actively decide about each month.</p>
        {fixedExpenses.length > 0 && (
          <ul className="expense-list">
            {fixedExpenses.map((expense) => (
              <li key={expense.id} className="expense-row">
                <div className="expense-row-main">
                  <span className="expense-description">{expense.name}</span>
                </div>
                <div className="expense-row-end">
                  <span className="expense-amount">{formatMoney(expense.monthlyAmount)}</span>
                  <button
                    type="button"
                    className="expense-delete"
                    aria-label={`Remove ${expense.name}`}
                    onClick={() => onDeleteFixedExpense(expense.id)}
                  >
                    ×
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
        <form className="expense-form-row" onSubmit={handleAddFixed}>
          <div className="field field-description">
            <label htmlFor="fixed-name">Name</label>
            <input
              id="fixed-name"
              type="text"
              placeholder="Rent, utilities, car payment…"
              value={fixedName}
              onChange={(e) => setFixedName(e.target.value)}
            />
          </div>
          <div className="field field-amount">
            <label htmlFor="fixed-amount">Monthly amount</label>
            <div className="field-amount-input">
              <span aria-hidden="true">$</span>
              <input
                id="fixed-amount"
                type="number"
                min="0.01"
                step="0.01"
                placeholder="0.00"
                value={fixedAmount}
                onChange={(e) => setFixedAmount(e.target.value)}
              />
            </div>
          </div>
          <button type="submit" className="btn-secondary" disabled={addingFixed}>
            {addingFixed ? "Adding…" : "Add"}
          </button>
        </form>
        <p className="budget-setup-hint">Total fixed expenses: {formatMoney(fixedTotal)}</p>
      </section>

      {/* Flexible categories: budget/spent progress per category, inline edit, and add form. */}
      <section className="card">
        <h2 className="card-title">Flexible categories</h2>
        <p className="budget-setup-hint">
        </p>
        {flexibleCategories.length > 0 && (
          <ul className="category-list">
            {flexibleCategories.map((category) => {
              // Inline edit form, shown instead of the display row while editing.
              if (editingCategoryId === category.id) {
                return (
                  <li key={category.id} className="category-row">
                    <form className="expense-form-row" onSubmit={(e) => handleSaveEditCategory(e, category.id)}>
                      <div className="field field-description">
                        <label htmlFor={`edit-name-${category.id}`}>Name</label>
                        <input
                          id={`edit-name-${category.id}`}
                          type="text"
                          value={editName}
                          onChange={(e) => setEditName(e.target.value)}
                          autoFocus
                        />
                      </div>
                      <div className="field field-amount">
                        <label htmlFor={`edit-budget-${category.id}`}>Monthly budget</label>
                        <div className="field-amount-input">
                          <span aria-hidden="true">$</span>
                          <input
                            id={`edit-budget-${category.id}`}
                            type="number"
                            min="0.01"
                            step="0.01"
                            value={editBudget}
                            onChange={(e) => setEditBudget(e.target.value)}
                          />
                        </div>
                      </div>
                      <button type="submit" className="btn-secondary" disabled={savingEdit}>
                        {savingEdit ? "Saving…" : "Save"}
                      </button>
                      <button type="button" className="btn-text" onClick={cancelEditingCategory}>
                        Cancel
                      </button>
                    </form>
                  </li>
                );
              }

              // Display row: name, remaining amount, progress bar, spent/budgeted line.
              const breakdown = categoryBreakdownById.get(category.id);
              const spent = Number(breakdown?.spentThisMonth ?? 0);
              const budget = Number(category.monthlyBudget);
              const remaining = Number(breakdown?.remaining ?? budget);
              const pct = budget > 0 ? Math.min((spent / budget) * 100, 100) : 0;
              const isOver = remaining < 0;
              return (
                <li key={category.id} className="category-row">
                  <div className="category-row-top">
                    <span className="expense-description">{category.name}</span>
                    <div className="expense-row-end">
                      <span className={`expense-amount ${isOver ? "is-negative-text" : ""}`}>
                        {isOver ? "–" : ""}
                        {formatMoney(Math.abs(remaining))} left
                      </span>
                      <button
                        type="button"
                        className="btn-text"
                        onClick={() => startEditingCategory(category)}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="expense-delete"
                        aria-label={`Remove ${category.name}`}
                        onClick={() => handleDeleteCategory(category.id)}
                      >
                        ×
                      </button>
                    </div>
                  </div>
                  <div className="category-progress-track">
                    <div
                      className={`category-progress-fill ${isOver ? "is-over" : ""}`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                  <span className="expense-date">
                    {formatMoney(spent)} spent of {formatMoney(budget)} budgeted
                  </span>
                </li>
              );
            })}
          </ul>
        )}
        {categoryError && <p className="field-error">{categoryError}</p>}
        <form className="expense-form-row" onSubmit={handleAddCategory}>
          <div className="field field-description">
            <label htmlFor="category-name">Name</label>
            <input
              id="category-name"
              type="text"
              placeholder="Food, entertainment, personal…"
              value={categoryName}
              onChange={(e) => setCategoryName(e.target.value)}
            />
          </div>
          <div className="field field-amount">
            <label htmlFor="category-budget">Monthly budget</label>
            <div className="field-amount-input">
              <span aria-hidden="true">$</span>
              <input
                id="category-budget"
                type="number"
                min="0.01"
                step="0.01"
                placeholder="0.00"
                value={categoryBudget}
                onChange={(e) => setCategoryBudget(e.target.value)}
              />
            </div>
          </div>
          <button type="submit" className="btn-secondary" disabled={addingCategory}>
            {addingCategory ? "Adding…" : "Add"}
          </button>
        </form>
      </section>

      {/* Read-only recap: how net income flows through fixed and flexible into savings. */}
      {summary && (
        <section className="card">
          <h2 className="card-title">Where it all goes</h2>
          <div className="tax-breakdown">
            <div className="tax-breakdown-row">
              <span>Net income this month</span>
              <span>{formatMoney(summary.netThisMonth)}</span>
            </div>
            <div className="tax-breakdown-row">
              <span>Fixed expenses</span>
              <span>-{formatMoney(summary.fixedTotal)}</span>
            </div>
            <div className="tax-breakdown-row">
              <span>Remaining after fixed</span>
              <span>{formatMoney(summary.remainingAfterFixed)}</span>
            </div>
            <div className="tax-breakdown-row">
              <span>Flexible allocated</span>
              <span>-{formatMoney(summary.flexibleAllocated)}</span>
            </div>
            <div className="tax-breakdown-row tax-breakdown-total">
              <span>Extra savings</span>
              <span>{formatMoney(summary.extraSavings)}</span>
            </div>
          </div>
        </section>
      )}
    </>
  );
}
