import FlexibleRemainingHero from "./FlexibleRemainingHero";
import ExpenseForm from "./ExpenseForm";
import ExpenseList from "./ExpenseList";

/** The daily-use page: one number, the ability to log a new expense, and recent transactions. */
export default function Home({ summary, expenses, flexibleCategories, onAddExpense, onDeleteExpense }) {
  return (
    <>
      {/* The big flexible-remaining number. */}
      <FlexibleRemainingHero summary={summary} />

      {/* Form for logging a new expense. */}
      <section className="card">
        <ExpenseForm categories={flexibleCategories} onAdd={onAddExpense} />
      </section>

      {/* Recent transactions list. */}
      <section className="card">
        <h2 className="card-title">Recent transactions</h2>
        <ExpenseList expenses={expenses} onDelete={onDeleteExpense} />
      </section>
    </>
  );
}
