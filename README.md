# Guilt-Free Spending Tracker

A personal budgeting app that answers three questions every month: **how much do I have
left to spend on myself, what did my fixed costs take, and what's left over as savings?**

You enter your salary, your fixed monthly costs (rent, utilities, car payment) and a
budget for each flexible category (food, entertainment, personal). The app works out
your take-home pay from the tax rules, then shows what's left to spend this month as
you log expenses.

- **Backend:** Java 17, Spring Boot 3, Spring Security, H2 (file-based, no setup)
- **Frontend:** React 19 + Vite, plain CSS, Geist font (self-hosted)
- **One account per install**, created on first visit. A fresh install starts empty, with no demo data.

---

## Features

**Home**
- The headline number: flexible spending left this month, across all categories,
  plus what that works out to per remaining day.
- A form to log an expense (amount, description, category, date).
- Recent transactions, newest first, with "Show all" and delete (with confirmation).

**History**
- A bar chart of logged spending for the past week, past 30 days, this month, last
  month or past year (one bar per day, or per month for the year view).
- Totals for that range: net income, fixed costs, spent, total out and the difference.

**Settings**
- **Income & taxes:** yearly salary, state tax rate, pay frequency and a recent payday,
  with a live tax breakdown that updates as you type.
- **Fixed expenses:** add, edit and remove recurring monthly costs.
- **Flexible categories:** add, edit and remove categories, each with a progress bar
  showing spent vs. budget.
- **Where it all goes:** net income, minus fixed, minus flexible budgets, equals extra savings.

**General**
- Login, plus a one-time sign-up on first visit (see [Accounts and login](#accounts-and-login)).
- Light and dark mode (follows your system setting).
- Loading placeholders, inline error messages with retry, and empty states.
- Keyboard focus outlines, screen-reader labels and announcements, and support for the
  "reduce motion" setting.
- Small transitions: the sliding tab indicator, page fades and growing chart bars.
- Works on phones.

---

## How the numbers are calculated

### Monthly budget

```
netThisMonth         = take-home pay landing in this calendar month (see below)
fixedTotal           = sum of fixed expenses
remainingAfterFixed  = netThisMonth - fixedTotal
flexibleAllocated    = sum of flexible category budgets
extraSavings         = remainingAfterFixed - flexibleAllocated

per category:  spentThisMonth = expenses logged in that category this month
               remaining      = budget - spentThisMonth   (can go negative)

Home headline  = flexibleAllocated - total flexible spent this month
Per day        = headline / days left in the month (counting today)
```

Nothing is stored as "this month's budget". Every number is recomputed on each request
from three things you set once (income, fixed expenses, categories) plus the expenses
logged so far, so a new month needs no reset. Overspending is shown as a negative
amount, not hidden at zero. See `SummaryController.java`.

### Take-home pay

```
taxableIncome      = grossAnnual - standardDeduction
federalTax         = marginal brackets (single filer, 2026 IRS figures)
ficaTax            = min(grossAnnual, ssWageBase) * 6.2%  +  grossAnnual * 1.45%
stateTax           = grossAnnual * yourStateRate / 100
netAnnual          = grossAnnual - federalTax - ficaTax - stateTax
netPerPaycheck     = netAnnual / paychecksPerYear
netThisMonth       = netPerPaycheck * paydays that land in this calendar month
```

Biweekly and weekly pay don't divide evenly into months, so the app counts the actual
paydays in each month, starting from one real payday you enter. About twice a year a
biweekly earner gets a third paycheck in a month, and the breakdown flags it as a bonus
paycheck. Monthly and semi-monthly pay are always 1 and 2 paychecks. See
`TaxCalculationService.java`. The 2026 brackets, standard deduction and Social Security
wage base come from IRS Revenue Procedure 2025-32 and ssa.gov, and are cited in the code.

### History

Spending in History is real: it's summed from dated expenses. Income and fixed costs are
not stored per date, so they're rebuilt from your current settings. This month, last
month and past year cover whole calendar months. Past week and past 30 days can start
mid-month, so their income and fixed figures are spread across the months the range
covers, and the page labels them as estimates. See `HistoryController.java`.

### Known simplifications

- Single filer only. No pre-tax deductions (401k, health insurance) and no Additional
  Medicare tax.
- The state tax rate is a flat percentage you enter (0% for states with no income tax);
  there are no per-state tax tables.
- Payday counting assumes pay never skips a period.
- One budget per install (see below).

---

## Accounts and login

- **First visit:** the app shows **Create your account**. The username and password are
  stored in the database, with the password hashed (bcrypt).
- **After that:** sign-up closes and only the login page is shown. There is exactly
  one account and one budget.
- **Every API call** except `/api/signup` needs that login, sent as HTTP Basic. The
  frontend keeps it in `sessionStorage`, so closing the tab logs you out. **Log out** in
  the header does the same.
- **No password reset.** If you forget it, delete `backend/data/` and sign up again.
  This also deletes the budget.

---

## Project structure

```
guilt-free-tracker/
  backend/                         Spring Boot API (Java 17, Maven)
    src/main/java/com/guiltfree/tracker/
      config/                      CORS and security (login, password hashing)
      controller/                  REST endpoints: summary, history, expenses, income,
                                   fixed expenses, categories, sign-up
      service/                     TaxCalculationService (tax and payday maths)
      model/  repository/  dto/    JPA entities, data access, request/response shapes
    src/test/                      Tax maths, budget API flow, sign-up/login tests
    Dockerfile                     Container build for deployment
  frontend/                        React app (Vite)
    src/App.jsx                    Login gate, tabs, loading/error states, shared data
    src/api.js                     REST client and login handling
    src/useAction.js               Busy/error state for buttons and forms
    src/components/                Login, History, Settings, expense form/list, hero number
    src/App.css, src/index.css     Styles; colour and font tokens (light and dark)
```

---

## Running it locally

You'll need **Java 17+ and Maven** for the backend, and **Node 18+** for the frontend.

### 1. Backend

```bash
cd backend
mvn spring-boot:run
```

Starts on `http://localhost:8080`. Data is saved in `backend/data/guiltfree.mv.db` and
survives restarts. Delete that folder to start over (also needed once after a schema
change).

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:5173` and calls the backend at `http://localhost:8080`.
On the first visit, create your account, then go to **Settings** to enter your salary,
fixed expenses and categories. Home and History show zeros until you do.

### Tests and checks

```bash
# in backend/
mvn test         # tax maths, budget API flow, sign-up/login (19 tests)

# in frontend/
npm run lint     # oxlint
npm run build    # production build
```

### Configuration

| Variable | Where | Default | Purpose |
|---|---|---|---|
| `PORT` | backend | `8080` | Server port |
| `FRONTEND_ORIGIN` | backend | `http://localhost:5173,http://localhost:3000` | Allowed frontend origins for CORS (comma-separated) |
| `VITE_API_BASE_URL` | frontend (build time) | `http://localhost:8080` | Backend URL |

---

## API

All endpoints except `/api/signup` need the login (HTTP Basic).

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/signup` | `{ "open": true }` while no account exists yet |
| POST | `/api/signup` | Create the one account: `{ "username", "password" }` (password 8-72 characters). Returns 409 once an account exists |
| GET | `/api/summary` | Net income, fixed total, extra savings, and every flexible category with spent/remaining |
| GET | `/api/history?range=THIS_MONTH` | Chart points and totals for `WEEK`, `THIRTY_DAYS`, `THIS_MONTH`, `LAST_MONTH` or `YEAR` (defaults to `THIS_MONTH`) |
| GET | `/api/expenses` | All expenses, newest first, with `categoryId` and `categoryName` |
| POST | `/api/expenses` | Log an expense: `{ "amount": 12.50, "description": "Coffee", "date": "2026-07-06", "categoryId": 1 }` (`date` optional, defaults to today) |
| DELETE | `/api/expenses/{id}` | Remove an expense |
| GET | `/api/income` | Saved salary, state tax rate, pay frequency and anchor payday |
| POST | `/api/income/estimate` | Tax breakdown preview; saves nothing |
| PUT | `/api/income` | Save the income profile; returns the breakdown |
| GET / POST | `/api/fixed-expenses` | List / add fixed expenses: `{ "name", "monthlyAmount" }` |
| PUT / DELETE | `/api/fixed-expenses/{id}` | Edit / remove a fixed expense |
| POST | `/api/flexible-categories` | Add a category: `{ "name", "monthlyBudget" }` (listed via `/api/summary`) |
| PUT / DELETE | `/api/flexible-categories/{id}` | Edit / remove a category. DELETE returns 409 if expenses are still logged against it |

Validation errors return 400; unknown ids return 404.

---

## Deploying

The repo is set up for Render or Railway (backend) and Vercel (frontend).

**Backend (Render or Railway)**
1. Create a new web service pointing at the `backend/` folder. It builds from the
   included `Dockerfile`.
2. Set `FRONTEND_ORIGIN` to your frontend URL so CORS allows it.
3. Note the backend URL, e.g. `https://your-app.onrender.com`.

**Frontend (Vercel)**
1. Import the `frontend/` folder as a new project (Vite is detected automatically).
2. Set `VITE_API_BASE_URL` to the backend URL.
3. Deploy.

**Then open the deployed app and create the account straight away.** Until you do,
anyone who finds the URL could claim it. Use a strong password. The free tiers on
Render/Railway sleep when idle, so the first request after a while can take 30-60
seconds.

The H2 database is a file inside the container. On hosts with temporary disks, the data
resets on every redeploy unless you attach a persistent disk (or switch to a hosted
database).
