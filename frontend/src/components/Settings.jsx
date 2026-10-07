import { useEffect, useState } from "react";
import { formatMoney, todayIso } from "../format";
import { useAction } from "../useAction";

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

// The income request body that both the estimate and save endpoints take, from the raw inputs.
function toIncome(salary, stateRate, payFrequency, anchorPayDate) {
  return {
    yearlySalary: Number(salary),
    stateTaxRatePercent: Number(stateRate) || 0,
    payFrequency,
    anchorPayDate: NEEDS_ANCHOR_DATE.has(payFrequency) ? anchorPayDate : null,
  };
}

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
  onUpdateFixedExpense,
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
  const [estimateError, setEstimateError] = useState(null);

  // One busy/error pair per card; the row being edited inline is tracked by id.
  const [savingIncome, incomeError, runIncome] = useAction();
  const [fixedBusy, fixedError, runFixed] = useAction();
  const [categoryBusy, categoryError, runCategory] = useAction();
  const [editingFixedId, setEditingFixedId] = useState(null);
  const [editingCategoryId, setEditingCategoryId] = useState(null);

  // Live preview: re-estimate whenever an income input changes, debounced so
  // we're not hitting the API on every keystroke.
  useEffect(() => {
    const income = toIncome(salary, stateRate, payFrequency, anchorPayDate);
    if (!(income.yearlySalary > 0)) {
      setBreakdown(null);
      return;
    }
    setEstimating(true);
    setEstimateError(null);
    const timeout = setTimeout(() => {
      onEstimateIncome(income)
        .then(setBreakdown)
        .catch((err) => {
          console.error(err);
          setBreakdown(null);
          setEstimateError("Couldn't calculate taxes for those numbers. Check them and try again.");
        })
        .finally(() => setEstimating(false));
    }, 400);

    return () => clearTimeout(timeout);
  }, [salary, stateRate, payFrequency, anchorPayDate, onEstimateIncome]);

  function deleteFixed(expense) {
    if (!window.confirm(`Remove "${expense.name}" from fixed expenses?`)) return;
    runFixed(() => onDeleteFixedExpense(expense.id), "Couldn't remove that expense. Try again.");
  }

  async function saveFixed(id, name, amount) {
    const ok = await runFixed(() => onUpdateFixedExpense(id, name, amount), "Couldn't save that change. Try again.");
    if (ok) setEditingFixedId(null);
    return ok;
  }

  function deleteCategory(category) {
    if (!window.confirm(`Delete the "${category.name}" category?`)) return;
    runCategory(
      () => onDeleteFlexibleCategory(category.id),
      "Can't delete that category - it still has expenses logged against it.",
    );
  }

  async function saveCategory(id, name, budget) {
    const ok = await runCategory(() => onUpdateFlexibleCategory(id, name, budget), "Couldn't save that change. Try again.");
    if (ok) setEditingCategoryId(null);
    return ok;
  }

  return (
    <>
      {/* Income & taxes: salary/rate/frequency inputs plus the live tax breakdown preview. */}
      <section className="card">
        <h2 className="card-title">Income &amp; taxes</h2>
        <div className="income-grid">
          <div className="field">
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
          <div className="field">
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
          <div className="field">
            <label htmlFor="pay-frequency">Pay frequency</label>
            <select
              id="pay-frequency"
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
            <div className="field">
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

        {estimating && !breakdown && <p className="budget-setup-hint">Calculating…</p>}
        {!salary && <p className="budget-setup-hint">Enter your yearly salary to see your take-home pay.</p>}
        {(estimateError || incomeError) && <p className="field-error" role="alert">{estimateError || incomeError}</p>}

        {/* Stays on screen (dimmed) while recalculating so the card doesn't jump. */}
        {breakdown && (
          <div className={`tax-breakdown ${estimating ? "is-loading" : ""}`} aria-busy={estimating}>
            <BreakdownRows
              rows={[
                ["Gross annual", formatMoney(breakdown.grossAnnual)],
                ["Federal tax", `−${formatMoney(breakdown.federalTax)}`],
                ["FICA (Social Security + Medicare)", `−${formatMoney(breakdown.ficaTax)}`],
                ["State tax", `−${formatMoney(breakdown.stateTax)}`],
                ["Net annual", formatMoney(breakdown.netAnnual)],
                ["Average monthly (net ÷ 12)", formatMoney(breakdown.netMonthlyAverage)],
              ]}
            />
            <div className="tax-breakdown-row tax-breakdown-total">
              <span>
                This month ({breakdown.paychecksThisMonth} paycheck{breakdown.paychecksThisMonth === 1 ? "" : "s"})
                {breakdown.paychecksThisMonth > USUAL_PAYCHECKS_PER_MONTH[payFrequency] && " (bonus paycheck)"}
              </span>
              <span>{formatMoney(breakdown.netThisMonth)}</span>
            </div>
          </div>
        )}

        <button
          type="button"
          className="btn-primary"
          onClick={() =>
            runIncome(
              () => onSaveIncome(toIncome(salary, stateRate, payFrequency, anchorPayDate)),
              "Couldn't save your income. Try again.",
            )
          }
          disabled={savingIncome || !breakdown}
        >
          {savingIncome ? "Saving…" : "Save income"}
        </button>
      </section>

      {/* Fixed expenses: recurring costs list plus the add form. */}
      <section className="card">
        <h2 className="card-title">Fixed expenses</h2>
        <p className="budget-setup-hint">Rent, utilities, car payment: costs you don't actively decide about each month.</p>
        {fixedExpenses.length > 0 && (
          <ul className="expense-list">
            {fixedExpenses.map((expense) =>
              // Inline edit form, shown instead of the row while editing (same as categories below).
              editingFixedId === expense.id ? (
                <li key={expense.id} className="expense-row">
                  <NameAmountForm
                    id={`edit-fixed-${expense.id}`}
                    amountLabel="Monthly amount"
                    initial={{ name: expense.name, amount: expense.monthlyAmount }}
                    busy={fixedBusy}
                    submitLabel={fixedBusy ? "Saving…" : "Save"}
                    onSubmit={(name, amount) => saveFixed(expense.id, name, amount)}
                  >
                    <button type="button" className="btn-text" onClick={() => setEditingFixedId(null)}>
                      Cancel
                    </button>
                  </NameAmountForm>
                </li>
              ) : (
                <li key={expense.id} className="expense-row">
                  <div className="expense-row-main">
                    <span className="expense-description">{expense.name}</span>
                  </div>
                  <div className="expense-row-end">
                    <span className="expense-amount">{formatMoney(expense.monthlyAmount)}</span>
                    <button type="button" className="btn-text" onClick={() => setEditingFixedId(expense.id)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className="expense-delete"
                      aria-label={`Remove ${expense.name}`}
                      onClick={() => deleteFixed(expense)}
                    >
                      ×
                    </button>
                  </div>
                </li>
              ),
            )}
          </ul>
        )}
        <NameAmountForm
          id="fixed"
          amountLabel="Monthly amount"
          placeholder="Rent, utilities, car payment…"
          busy={fixedBusy}
          submitLabel={fixedBusy ? "Adding…" : "Add"}
          onSubmit={(name, amount) => runFixed(() => onAddFixedExpense(name, amount), "Couldn't add that expense. Try again.")}
        />
        {fixedError && <p className="field-error" role="alert">{fixedError}</p>}
        <p className="budget-setup-hint">Total fixed expenses: {formatMoney(summary?.fixedTotal)}</p>
      </section>

      {/* Flexible categories: budget/spent progress per category, inline edit, and add form. */}
      <section className="card">
        <h2 className="card-title">Flexible categories</h2>
        {flexibleCategories.length > 0 && (
          <ul className="category-list">
            {flexibleCategories.map((category) => {
              // Inline edit form, shown instead of the display row while editing.
              if (editingCategoryId === category.id) {
                return (
                  <li key={category.id} className="category-row">
                    <NameAmountForm
                      id={`edit-${category.id}`}
                      amountLabel="Monthly budget"
                      initial={{ name: category.name, amount: category.monthlyBudget }}
                      busy={categoryBusy}
                      submitLabel={categoryBusy ? "Saving…" : "Save"}
                      onSubmit={(name, budget) => saveCategory(category.id, name, budget)}
                    >
                      <button type="button" className="btn-text" onClick={() => setEditingCategoryId(null)}>
                        Cancel
                      </button>
                    </NameAmountForm>
                  </li>
                );
              }

              // Display row: name, remaining amount, progress bar, spent/budgeted line.
              const spent = Number(category.spentThisMonth);
              const budget = Number(category.monthlyBudget);
              const remaining = Number(category.remaining);
              const pct = budget > 0 ? Math.min((spent / budget) * 100, 100) : 0;
              const isOver = remaining < 0;
              return (
                <li key={category.id} className="category-row">
                  <div className="category-row-top">
                    <span className="expense-description">{category.name}</span>
                    <div className="expense-row-end">
                      <span className={`expense-amount ${isOver ? "is-negative-text" : ""}`}>
                        {isOver ? "−" : ""}
                        {formatMoney(Math.abs(remaining))} left
                      </span>
                      <button type="button" className="btn-text" onClick={() => setEditingCategoryId(category.id)}>
                        Edit
                      </button>
                      <button
                        type="button"
                        className="expense-delete"
                        aria-label={`Remove ${category.name}`}
                        onClick={() => deleteCategory(category)}
                      >
                        ×
                      </button>
                    </div>
                  </div>
                  <div className="category-progress-track">
                    <div
                      className={`category-progress-fill ${isOver ? "is-over" : ""}`}
                      style={{ transform: `scaleX(${pct / 100})` }}
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
        {categoryError && <p className="field-error" role="alert">{categoryError}</p>}
        <NameAmountForm
          id="category"
          amountLabel="Monthly budget"
          placeholder="Food, entertainment, personal…"
          busy={categoryBusy}
          submitLabel={categoryBusy ? "Adding…" : "Add"}
          onSubmit={(name, budget) => runCategory(() => onAddFlexibleCategory(name, budget), "Couldn't add that category. Try again.")}
        />
      </section>

      {/* Read-only recap: how net income flows through fixed and flexible into savings. */}
      {summary && (
        <section className="card">
          <h2 className="card-title">Where it all goes</h2>
          <div className="tax-breakdown">
            <BreakdownRows
              rows={[
                ["Net income this month", formatMoney(summary.netThisMonth)],
                ["Fixed expenses", `−${formatMoney(summary.fixedTotal)}`],
                ["Remaining after fixed", formatMoney(summary.remainingAfterFixed)],
                ["Flexible allocated", `−${formatMoney(summary.flexibleAllocated)}`],
              ]}
            />
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

// Name + monthly-amount form: adding fixed expenses, adding and editing categories.
// Uncontrolled - values are read on submit, and the form resets when onSubmit resolves true.
function NameAmountForm({ id, amountLabel, placeholder, initial, busy, submitLabel, onSubmit, children }) {
  async function handleSubmit(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    if (await onSubmit(data.get("name").trim(), Number(data.get("amount")))) form.reset();
  }

  return (
    <form className="expense-form-row" onSubmit={handleSubmit}>
      <div className="field field-description">
        <label htmlFor={`${id}-name`}>Name</label>
        <input
          id={`${id}-name`}
          name="name"
          type="text"
          required
          pattern=".*\S.*"
          placeholder={placeholder}
          defaultValue={initial?.name}
          autoFocus={Boolean(initial)}
        />
      </div>
      <div className="field field-amount">
        <label htmlFor={`${id}-amount`}>{amountLabel}</label>
        <div className="field-amount-input">
          <span aria-hidden="true">$</span>
          <input
            id={`${id}-amount`}
            name="amount"
            type="number"
            required
            min="0.01"
            step="0.01"
            placeholder="0.00"
            defaultValue={initial?.amount}
          />
        </div>
      </div>
      <button type="submit" className="btn-secondary" disabled={busy}>
        {submitLabel}
      </button>
      {children}
    </form>
  );
}

// Plain label/value rows inside a .tax-breakdown block.
function BreakdownRows({ rows }) {
  return rows.map(([label, value]) => (
    <div key={label} className="tax-breakdown-row">
      <span>{label}</span>
      <span>{value}</span>
    </div>
  ));
}
