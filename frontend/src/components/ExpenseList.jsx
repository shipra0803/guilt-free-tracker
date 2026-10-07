import { useState } from "react";
import { formatMoney, formatShortDate } from "../format";
import { useAction } from "../useAction";

const RECENT_COUNT = 8;

// Renders the recent-expenses list on the Home page, or an empty-state message.
export default function ExpenseList({ expenses, onDelete }) {
  const [showAll, setShowAll] = useState(false);
  const [, error, run] = useAction();

  // Empty state - no expenses logged yet.
  if (!expenses || expenses.length === 0) {
    return <p className="empty-state">No expenses logged yet. Add your first one above.</p>;
  }

  function handleDelete(expense) {
    if (!window.confirm(`Delete "${expense.description}" (${formatMoney(expense.amount)})?`)) return;
    run(() => onDelete(expense.id), "Couldn't delete that expense. Try again.");
  }

  const visible = showAll ? expenses : expenses.slice(0, RECENT_COUNT);

  // Most recent expenses (or all of them) with a delete button on each row.
  return (
    <>
      <ul className="expense-list">
        {visible.map((expense) => (
          <li key={expense.id} className="expense-row">
            <div className="expense-row-main">
              <span className="expense-description">{expense.description}</span>
              <span className="expense-date">
                {formatShortDate(expense.date)} · {expense.categoryName}
              </span>
            </div>
            <div className="expense-row-end">
              <span className="expense-amount">{formatMoney(expense.amount)}</span>
              <button
                type="button"
                className="expense-delete"
                aria-label={`Delete ${expense.description}`}
                onClick={() => handleDelete(expense)}
              >
                ×
              </button>
            </div>
          </li>
        ))}
      </ul>
      {error && <p className="field-error" role="alert">{error}</p>}
      {expenses.length > RECENT_COUNT && (
        <button type="button" className="btn-text" onClick={() => setShowAll(!showAll)}>
          {showAll ? "Show recent only" : `Show all ${expenses.length}`}
        </button>
      )}
    </>
  );
}
