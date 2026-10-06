import { useEffect, useState } from "react";
import { todayIso } from "../format";

/**
 * Logging an expense is the other half of the demo: submit here, and the Home
 * page's "flexible remaining" number recalculates immediately from the fresh total.
 * Every expense belongs to a Flexible category - there's no "uncategorized" option,
 * since fixed costs live only in the separate recurring list managed in Settings.
 */
export default function ExpenseForm({ categories, onAdd }) {
  // Form field state, plus submitting/error status.
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [date, setDate] = useState(todayIso());
  const [categoryId, setCategoryId] = useState(categories?.[0]?.id ?? "");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Keep the selected category valid as the list loads or changes.
  useEffect(() => {
    if (!categories || categories.length === 0) {
      setCategoryId("");
      return;
    }
    if (!categories.some((c) => String(c.id) === String(categoryId))) {
      setCategoryId(categories[0].id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [categories]);

  const hasCategories = categories && categories.length > 0;

  // Validates the form, submits the new expense, and resets on success.
  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);

    if (!hasCategories) {
      setError("Create a flexible category in Settings before logging an expense.");
      return;
    }
    const parsedAmount = Number(amount);
    if (!parsedAmount || parsedAmount <= 0) {
      setError("Enter an amount greater than zero.");
      return;
    }
    if (!description.trim()) {
      setError("Give it a short description.");
      return;
    }

    setSubmitting(true);
    try {
      await onAdd({
        amount: parsedAmount,
        description: description.trim(),
        date,
        categoryId: Number(categoryId),
      });
      setAmount("");
      setDescription("");
      setDate(todayIso());
    } catch (err) {
      setError("Couldn't save that expense. Is the backend running?");
      console.error(err);
    } finally {
      setSubmitting(false);
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
                  type="number"
                  inputMode="decimal"
                  min="0.01"
                  step="0.01"
                  placeholder="0.00"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                />
              </div>
            </div>
            <div className="field field-description">
              <label htmlFor="description">Description</label>
              <input
                id="description"
                type="text"
                placeholder="Coffee with a friend"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <div className="field field-description">
              <label htmlFor="category">Category</label>
              <select
                id="category"
                className="pay-frequency-select"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field field-date">
              <label htmlFor="date">Date</label>
              <input id="date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
            </div>
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? "Adding…" : "Add expense"}
            </button>
          </div>
          {error && <p className="field-error">{error}</p>}
        </>
      )}
    </form>
  );
}
