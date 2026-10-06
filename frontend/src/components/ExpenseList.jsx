import { formatMoney, formatShortDate } from "../format";

// Renders the recent-expenses list on the Home page, or an empty-state message.
export default function ExpenseList({ expenses, onDelete }) {
  // Empty state - no expenses logged yet.
  if (!expenses || expenses.length === 0) {
    return <p className="empty-state">No expenses logged yet — add your first one above.</p>;
  }

  // Shows the 8 most recent expenses with a delete button on each row.
  return (
    <ul className="expense-list">
      {expenses.slice(0, 8).map((expense) => (
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
              onClick={() => onDelete(expense.id)}
            >
              ×
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}
