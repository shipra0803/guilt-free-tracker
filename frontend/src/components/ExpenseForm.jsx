import { todayIso } from "../format";
import { useAction } from "../useAction";

/**
 * Logging an expense is the other half of the demo: submit here, and the Home
 * page's "flexible remaining" number recalculates immediately from the fresh total.
 * Every expense belongs to a Flexible category - there's no "uncategorized" option,
 * since fixed costs live only in the separate recurring list managed in Settings.
 */
export default function ExpenseForm({ categories, onAdd }) {
  const [submitting, error, run] = useAction();
  const hasCategories = categories && categories.length > 0;

  // Uncontrolled: reads the fields on submit (their required/min attributes validate them),
  // then clears them on success - keeping the chosen category for the next entry.
  async function handleSubmit(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const expense = {
      amount: Number(data.get("amount")),
      description: data.get("description").trim(),
      date: data.get("date"),
      categoryId: Number(data.get("category")),
    };
    if (await run(() => onAdd(expense), "Couldn't save that expense. Try again.")) {
      form.reset();
      form.elements.category.value = String(expense.categoryId);
    }
  }
  // Renders either a prompt to create a category first, or the full form.
  return (
    <form className="expense-form" onSubmit={handleSubmit}>
      <h2 className="card-title">Log an expense</h2>
      {!hasCategories ? (
        <p className="empty-state">
          Create a flexible category (Food, Entertainment, Personal…) in Settings before logging an expense.
        </p>
      ) : (
        <>
          <div className="expense-form-row">
            <div className="field field-amount">
              <label htmlFor="amount">Amount</label>
              <div className="field-amount-input">
                <span aria-hidden="true">$</span>
                <input
                  id="amount"
                  name="amount"
                  type="number"
                  inputMode="decimal"
                  required
                  min="0.01"
                  step="0.01"
                  placeholder="0.00"
                />
              </div>
            </div>
            <div className="field field-description">
              <label htmlFor="description">Description</label>
              <input
                id="description"
                name="description"
                type="text"
                required
                pattern=".*\S.*"
                placeholder="Coffee with a friend"
              />
            </div>
            <div className="field field-description">
              <label htmlFor="category">Category</label>
              <select id="category" name="category">
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field field-date">
              <label htmlFor="date">Date</label>
              <input id="date" name="date" type="date" required defaultValue={todayIso()} />
            </div>
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? "Adding…" : "Add expense"}
            </button>
          </div>
          {error && <p className="field-error" role="alert">{error}</p>}
        </>
      )}
    </form>
  );
}
