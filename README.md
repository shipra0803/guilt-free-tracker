# Guilt-Free Spending Tracker

Answers three questions every month: **how much do I have left to spend on myself,
what did my fixed costs take, and what's left over as savings?** Income and taxes feed
fixed expenses and flexible category budgets; whatever's left after both is Extra
savings. Three pages: **Home** (the number you check daily, plus recent transactions),
**History** (a spending chart and income-vs-expenses totals over a range you pick — past
week, past 30 days, this month, last month, or past year), and **Settings** (where
income, fixed expenses, and flexible category budgets all get set up).

- Backend: Java 17 + Spring Boot 3, REST API, H2 (file-based, zero setup)
- Frontend: React (Vite), three in-app pages (Home / History / Settings), no auth
- No seed/demo data — a fresh install starts completely empty; you set up your own income, fixed expenses, and flexible categories on the Settings page

---

## The core logic (whiteboard version)

```
netThisMonth        = from income + taxes (see below)
fixedTotal           = sum(fixed expenses)                       <- rent, utilities, car payment
remainingAfterFixed  = netThisMonth - fixedTotal
flexibleAllocated    = sum(flexible category budgets)            <- food, entertainment, personal spending
extraSavings         = remainingAfterFixed - flexibleAllocated   <- recalculated fresh each month, doesn't carry a balance forward

per flexible category: spentThisMonth = sum(expenses logged in that category this month)
                        remaining      = category budget - spentThisMonth   (never clamped at zero - overspend is visible, not hidden)
```

Nothing here is a stored "this month's budget" row. Every number is computed live from
three things that persist indefinitely on their own — your income profile, your fixed
expenses list, and your flexible category list — plus whatever's actually been logged
this month. That's it: three inputs you set once (and edit whenever), one calculation,
recomputed from scratch on every page load. Lives in `CategoryBudgetService.java`.

**Why this replaced a single "guilt-free number."** The original version of this app
answered "how much can I spend *today*?" with one number that redistributed
over/underspending across the rest of the month. It worked, but during testing a big
one-time cost (rent, logged as a regular expense) would tank that single number for two
weeks straight — a legitimate demonstration of the math, but a rough first impression.
Splitting spending into Fixed / Flexible / Savings buckets means a big cost either
belongs in Fixed (it never touches a "how much can I spend on myself" number at all) or
in a bounded Flexible category (going over just shows that one category negative,
without dragging down everything else). The Home page's headline number is now the
aggregate Flexible remaining for the month, not a daily figure — a monthly allowance
you check in on, not something recalculated to the day.

The Home page's hero number is `flexibleAllocated - flexibleSpentThisMonth`, summed
across every category. Settings shows the same number broken out per category, each
with its own spent/remaining and a progress bar.

---

## Setting your budget from salary, fixed expenses, and flexible categories

Everything is set up on the **Settings** page, in three sections:

```
taxableIncome      = grossAnnual - standardDeduction
federalTax         = marginal bracket calculation (single filer, 2026 IRS figures)
ficaTax            = min(grossAnnual, ssWageBase) * 6.2%  +  grossAnnual * 1.45%
stateTax           = grossAnnual * yourStateTaxRate / 100      <- you enter this yourself
netAnnual          = grossAnnual - federalTax - ficaTax - stateTax
netPerPaycheck     = netAnnual / paychecksPerYear(payFrequency)
paychecksThisMonth = count of real paydays landing in the current calendar month
netThisMonth       = netPerPaycheck * paychecksThisMonth
```

**Why "this month" and not just "annual ÷ 12":** biweekly and weekly pay don't divide
evenly into months. A biweekly earner gets paid every 14 days regardless of the
calendar, so roughly twice a year one month gets a 3rd "bonus" paycheck. `PayScheduleService`
walks every day in the target month from your anchor pay date (the date of any one real
paycheck) and counts how many paydays actually land in it, so `netThisMonth` reflects
what actually hits your account, not a smoothed average. Monthly and semi-monthly pay
are exact (1 and 2 paychecks respectively, every month, no anchor date needed).

Fixed expenses and flexible categories are both simple flat lists — add a name and a
monthly amount/budget, remove when done. Deleting a flexible category that still has
expenses logged against it is blocked (you'll need to remove or reassign those first),
since every logged expense must belong to exactly one category.

Lives in `TaxCalculationService.java` (tax + paycheck math), `PayScheduleService.java`
(the paycheck-calendar counting), and `CategoryBudgetService.java` (the fixed/flexible/
savings breakdown). **Note:** this logic was previously covered by a JUnit test suite
(`PayScheduleServiceTest`, `TaxCalculationServiceTest`, `CategoryBudgetServiceTest`,
`HistoryServiceTest`) with hand-worked expected values, but `backend/src/test/` is not
currently present in this copy of the project — see the project documentation for
details if you're picking this back up.

**Deliberate simplifications**, stated in the code and worth knowing before the interview:
- Single filer only, no pre-tax deductions (401k, health insurance), no Additional
  Medicare surtax over $200k.
- **State tax is not looked up from a table.** You enter your own flat state tax rate
  (0% is valid and expected for the nine states with no wage income tax) — a scoping
  call to avoid embedding and maintaining 50 states of bracket data for a demo.
- Federal brackets, the standard deduction, and the Social Security wage base are
  sourced directly from IRS Revenue Procedure 2025-32 and ssa.gov, tax year 2026 —
  cited in code comments in `TaxCalculationService.java`.
- Biweekly/weekly "bonus paycheck" months are detected from a single anchor pay date
  you provide, assuming pay never skips a period (no unpaid leave, no schedule
  changes). Good enough for a demo; a real payroll integration would replace this.
- No sub-categories within a Flexible category — one shared pool per category, same
  "keep it simple" philosophy the app started with, just applied per-category now.

**Persistence across months:** income, fixed expenses, and flexible category budgets
are all saved once and apply going forward automatically — nothing is scoped to "this
month" and forgotten. Since there's no stored "budget" row anymore (everything is
computed live from these three inputs), there's no class of bug where a new month
silently resets to zero — there's nothing dated to go stale in the first place.

Design notes and the full decision trail previously lived in
`docs/2026-07-02-category-budget-pivot-design.md` (this pivot) and
`docs/2026-07-02-salary-tax-budget-design.md` (the original salary/tax feature), but
the `docs/` folder is not currently present in this copy of the project.

---

## History page

Pick a range — **past week**, **past 30 days**, **this month**, **last month**, or
**past year** — and get a bar chart of your actual logged spending over that window,
plus totals for net income, fixed costs, spending, total money out, and the
difference. WEEK/THIRTY_DAYS/THIS_MONTH/LAST_MONTH chart one bar per day; YEAR charts
one bar per month (12 trailing months).

**Known simplification:** income and fixed costs aren't snapshotted per date, so
they're reconstructed from *today's* settings applied retroactively, not necessarily
what they were at the time — only the spending total is real, summed from actual dated
expenses. THIS_MONTH, LAST_MONTH, and YEAR line up with whole calendar months, so
their income/fixed figures are exact under that simplification. WEEK and THIRTY_DAYS
are rolling windows that can start and end mid-month, so their income/fixed figures
are prorated across every month the window touches and flagged `estimated: true` in
the API response (and called out in the UI) — there's no single "correct" answer for
what a partial month's paycheck or rent should count as.

`GET /api/history?range=WEEK|THIRTY_DAYS|THIS_MONTH|LAST_MONTH|YEAR` (defaults to
`THIS_MONTH`). On a fresh install with no logged expenses yet, every range shows zero
spending until you start using the app.

---

## Project structure

```
guilt-free-tracker/
  backend/    Spring Boot API (Java 17, Maven)
  frontend/   React app - Home / History / Settings pages (Vite)
  README.md   this file
```

---

## Running it locally

You'll need **Java 17+ and Maven** for the backend, and **Node 18+** for the frontend.
(This was authored in a sandbox without a JDK/Maven available, so it can't be compiled
there. The original MVP and the first salary/tax feature have both been run for real on
your machine already, with real bugs found and fixed there — see "Talking points"
below. This category pivot is newer and has only been verified via independently
checked math and a mock server matching the real API contract exactly — please run
`mvn spring-boot:run` and `mvn test` after pulling it and flag anything that doesn't
compile or behave as expected.)

### 1. Backend

```bash
cd backend
mvn spring-boot:run
```

Starts on `http://localhost:8080` with a completely empty database — no seed/demo
data. Go to the Settings page first to enter your salary, fixed expenses, and
flexible categories; Home and History will be blank/zero until you do.

Data persists in `backend/data/guiltfree.mv.db` between restarts. Delete that folder
to wipe everything and start over (also needed once after pulling a schema change,
same as any local database).

`mvn test` will currently report no tests found — `backend/src/test/` is not present in
this copy of the project (it previously held pure-calculation unit tests, no DB
needed).

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:5173` and talks to `http://localhost:8080` by default
(see `frontend/.env`).

---

## API

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/summary` | Net income, fixed total, extra savings, and the flexible category list with spent/remaining per category — powers Home and Settings |
| GET | `/api/history?range=THIS_MONTH` | Spending chart + range totals (net income, fixed, spent, total out, difference) for `WEEK`, `THIRTY_DAYS`, `THIS_MONTH`, `LAST_MONTH`, or `YEAR` |
| GET | `/api/expenses` | Recent expenses, newest first, each with `categoryId`/`categoryName` |
| POST | `/api/expenses` | Log an expense — `{ "amount": 12.50, "description": "Coffee", "date": "2026-07-06", "categoryId": 1 }` (`date` optional, defaults to today; `categoryId` required) |
| DELETE | `/api/expenses/{id}` | Remove an expense |
| GET | `/api/income` | Saved salary, state tax rate, pay frequency, anchor pay date |
| POST | `/api/income/estimate` | Live tax breakdown preview, doesn't save anything |
| PUT | `/api/income` | Save income + pay frequency, persists until changed |
| GET/POST/PUT/DELETE | `/api/fixed-expenses` | Rent, utilities, car payment — a flat list |
| GET/POST/PUT/DELETE | `/api/flexible-categories` | Food, entertainment, personal spending — a flat list with budgets; DELETE returns 409 if the category still has expenses logged against it |

---

## Talking points for the interview

- The whole app is three numbers and one subtraction chain: net income minus fixed
  expenses minus flexible category budgets equals extra savings. Nothing about "this
  month" is stored — income, fixed expenses, and flexible categories all persist
  indefinitely on their own, and every dashboard number is recomputed live from them,
  so there's no stale-row bug class to worry about across a month boundary.
- This category model replaced an earlier single "guilt-free number" that recalculated
  a daily allowance from remaining budget ÷ days left, and later a flat-rate running
  balance. Both were mathematically sound but a single number meant one big expense
  (rent) could tank it for the whole app. Splitting into Fixed / Flexible / Savings
  means a big cost either never touches a daily number (Fixed) or is contained to one
  bounded category (Flexible) — a good story about redesigning around what actually
  confused a user in testing, twice, rather than defending the original idea.
- **A category can go over budget on purpose, and it's shown as a negative number, not
  clamped to zero.** Hiding overspend would defeat the point of tracking it.
- The salary/tax engine (`TaxCalculationService`, `PayScheduleService`) is completely
  unchanged by this pivot — it just now feeds `CategoryBudgetService` instead of the
  old `BudgetService`. Good example of isolating a calculation behind a clean
  interface so a big surrounding redesign doesn't have to touch it at all.
- The History page is explicit about its own limitation: it reconstructs past months'
  income from today's settings rather than a real snapshot, because there's no
  month-scoped income history yet. Worth surfacing unprompted if asked how it works —
  it's a real, known gap, not something to be caught out on.
- The state tax rate being a manual input instead of a 50-state bracket table is a
  real scoping call, not a shortcut taken by accident — worth explaining the
  cost/benefit if asked.
- Retiring the old daily-number engine (`GuiltFreeCalculationService`, the `Budget`
  entity, the pace indicator) was a deliberate choice to delete rather than leave
  dormant, once the decision to pivot was made — avoids a second, dead budgeting
  engine sitting in the codebase that's awkward to explain if someone opens that file.
- (If you want a debugging story: H2's stricter SQL parser treats bare `YEAR`/`MONTH`/`DATE`
  as reserved keywords, so the original `Budget`/`Expense` columns literally named
  `year`, `month`, and `date` failed at `CREATE TABLE` with a cryptic "expected
  identifier" error. Fixed by renaming the physical columns via `@Column(name = ...)`
  while leaving the Java property names untouched.)

---

## Deploying later (optional, once you're ready to go live)

Deployment wasn't done as part of this build — you'll need to log into your own
Vercel/Render or Railway accounts, which I can't do on your behalf. The repo is
already set up for it, so when you're ready:

**Backend (Render or Railway):**
1. Push `backend/` to a GitHub repo.
2. Render/Railway: "New Web Service" → point at the repo/`backend` folder → it will
   detect the included `Dockerfile` and build automatically.
3. Set the `FRONTEND_ORIGIN` environment variable to your Vercel URL once you have it
   (comma-separated if you need more than one), so CORS allows the deployed frontend.
4. Note the resulting backend URL, e.g. `https://your-app.onrender.com`.

**Frontend (Vercel):**
1. Import the `frontend/` folder as a new Vercel project (it auto-detects Vite via
   the included `vercel.json`).
2. Set an environment variable `VITE_API_BASE_URL` to your backend URL from above.
3. Deploy — you'll get a live `https://your-app.vercel.app` link.

Render/Railway's free tiers spin down when idle and take ~30-60s to wake up on the
first request — worth loading the link a minute before you go live in an interview.
# guilt-free-tracker
# guilt-free-tracker
