# 🎩 Budget Butler

A witty, brutally honest budgeting app for freelancers and small business owners.
Built with **Spring Boot 3 (Java 17)**, **Angular 17 (standalone components)**, **MySQL**,
and **Spring Security + JWT** for multi-user login.

Every account has its own private categories, transactions, and dream — no login means
no access, and no user can see another user's data.

Every new account gets a **20-day free trial** with full Premium access. After that, the app
drops to whatever the user has actually paid for:

| Tier | Price | Unlocks |
|---|---|---|
| 🆓 **Free** | $0 forever | Categories, transactions, dream (numbers only) - no commentary |
| ⭐ **Plus** | $4.99/mo or $47.99/yr | + Daily Title, Twin Comparison commentary, Pace Analysis |
| 👑 **Premium** | $7.99/mo or $76.99/yr | + Dream Protector, Memory Lane, Talking Reports, Payday Patterns, Time Cost, Dream Garden |

Three features make Premium feel like a companion rather than a tracker:
- **💰 Payday Patterns** — detects how your spending shifts right after you get paid
- **⏳ Time Cost of Purchases** — shows what every expense costs you in hours of work
- **🌱 Dream Garden** — a virtual garden that grows or wilts with your financial habits
- **🔥 Streaks & Badges** — tracks consecutive under-budget days and awards achievement badges

Two more features are core (available on every tier, including Free):
- **👛 Wallets** — real money accounts (bank, cash, savings...). Every category belongs to a
  wallet; every expense debits that wallet automatically, and your salary refills whichever
  wallet you choose.
- **💱 Currency** — pick your currency once in Settings; it's applied everywhere in the app.

The app is also a **PWA (Progressive Web App)** — it can be installed on a phone's home
screen and opens like a real app, with no App Store or Play Store needed. See "📲 Installing
on your phone" below.

**Also added:** multiple dream goals at once, recurring expenses (rent/subscriptions log
themselves automatically), rate limiting on login/register, paginated transactions + CSV
export, and a full password reset + email verification flow (with a zero-setup console
fallback if you haven't configured SMTP yet).

---

## 📲 Installing on your phone (PWA)

Budget Butler is a **Progressive Web App** - once it's deployed to a real HTTPS URL (see
`DEPLOYMENT.md`), it can be installed on a phone's home screen like a native app: its own
icon, no browser address bar, works offline for the app shell. This costs $0 and needs no
App Store or Play Store review - a real advantage when your budget is $0.

**Important:** installability requires HTTPS. It will NOT work over plain `http://localhost`
in the same way real devices see it - test the installed experience after deploying (Netlify
and most hosts give you free HTTPS automatically).

### On Android (Chrome)
Visit the site → a banner appears automatically ("Install Budget Butler") thanks to the
`beforeinstallprompt` handling in `app.component.ts` → tap **Install**. It's added to the
home screen and opens in its own window, no browser UI.

### On iPhone (Safari)
iOS does not support the automatic install banner (Apple's choice, not something we can fix
in code) - it's a manual, one-time action:
1. Open the site in **Safari** (must be Safari, not Chrome on iOS).
2. Tap the **Share** icon (square with an arrow) at the bottom.
3. Scroll down and tap **Add to Home Screen**.
4. It now opens standalone, full-screen, with its own icon - indistinguishable from a
   downloaded app to the average user.

### On Desktop (Chrome/Edge)
An install icon appears in the address bar (or use the banner) - installs as a standalone
desktop window, appears in your OS's app list/dock like any other application.

### How this was built (for learning)
- `manifest.webmanifest` - tells the browser the app's name, icon, colors, and that it should
  open in `"display": "standalone"` (no browser chrome) instead of a normal browser tab.
- `ngsw-config.json` + `@angular/service-worker` - Angular's built-in service worker caches
  the app shell (HTML/CSS/JS/icons) so it loads instantly on repeat visits and works even with
  a flaky connection. It's only enabled in production builds (`isDevMode()` check in
  `app.config.ts`) so it doesn't interfere with local development.
- `assets/icons/` - a set of icons in the sizes different platforms expect (192px, 512px,
  maskable variants for Android's adaptive icon shapes, and a 180px Apple touch icon for iOS).
- The install banner in `app.component.ts` listens for the browser's `beforeinstallprompt`
  event and shows our own styled button instead of relying on the browser's easy-to-miss
  default install icon.

### What a PWA can't do (the honest limits)
- **No App Store / Play Store listing** - people won't find it by searching those stores.
  It only installs via a direct link (which is exactly what your marketing links point to).
- **iOS push notifications** only became possible in iOS 16.4+ and still need extra setup
  we haven't built here.
- If you outgrow this and want a true native app later (deeper OS integration, app store
  discovery), the code you'd reuse is 100% the backend - only the frontend would need
  rebuilding, e.g. with React Native or Flutter.

---

## 📁 Project structure

```
budget-butler/
├── backend/     ← Spring Boot API (Java)
└── frontend/    ← Angular app (TypeScript)
```

The two run as **separate processes**: backend on port `8080`, frontend on port `4200`.
The Angular app calls the Spring Boot API over HTTP, attaching a JWT token to every
request after login.

---

## 1) Install prerequisites

- **Java 17** (check with `java -version`)
- **Maven** (IntelliJ has this built in, so you likely don't need to install it separately)
- **Node.js 18+ and npm** (check with `node -v` and `npm -v`)
- **Angular CLI**: `npm install -g @angular/cli`
- **MySQL** running locally (MySQL Workbench or just the MySQL server is fine)
- **IntelliJ IDEA** (Community edition works fine for the backend)

---

## 2) Set up MySQL

You don't need to manually create the database or tables — Spring Boot does that for you
automatically the first time it runs (thanks to `createDatabaseIfNotExist=true` and
`spring.jpa.hibernate.ddl-auto=update` in `application.properties`).

You only need to make sure MySQL itself is running, and that the username/password in
`backend/src/main/resources/application.properties` match your local MySQL setup:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/budget_butler?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
```

Change `username`/`password` to whatever your local MySQL uses.

---

## 3) Set up Stripe (for the subscription/paywall feature)

1. Create a free account at [stripe.com](https://stripe.com) (test mode is fine, no real business needed to start).
2. Go to **Developers → API keys** → copy your **Secret key** (starts with `sk_test_...`).
3. Go to **Product catalog** and create **4 recurring Prices** (2 products - Plus and Premium -
   each with a monthly and an annual price):
   - Plus: $4.99/month, and $47.99/year (~20% off)
   - Premium: $7.99/month, and $76.99/year (~20% off)

   Copy each **Price ID** (starts with `price_...`) - you'll need all 4.
4. Install the [Stripe CLI](https://stripe.com/docs/stripe-cli) and run, in a separate terminal:
   ```bash
   stripe listen --forward-to localhost:8080/api/subscription/webhook
   ```
   This prints a **webhook signing secret** (starts with `whsec_...`) - copy it too.
5. Paste all the values into `backend/src/main/resources/application.properties`:
   ```properties
   stripe.secret-key=sk_test_...
   stripe.price-id-plus-monthly=price_...
   stripe.price-id-plus-annual=price_...
   stripe.price-id-premium-monthly=price_...
   stripe.price-id-premium-annual=price_...
   stripe.webhook-secret=whsec_...
   ```

You can skip this section entirely while first exploring the app - everything works without
Stripe configured, except clicking "Choose Plus/Premium" will fail until these are set.

---

## 4) Run the backend (in IntelliJ)

1. Open IntelliJ → **File → Open** → select the `backend` folder.
2. IntelliJ will detect it's a Maven project and download the dependencies automatically
   (this needs an internet connection the first time).
3. Open `BudgetButlerApplication.java` (in `src/main/java/com/budgetbutler`).
4. Click the green ▶️ Run button next to the `main` method.
5. You should see in the console: `Budget Butler backend is running on http://localhost:8080`

Test it worked by opening this in your browser: `http://localhost:8080/api/categories`
You should see an empty list: `[]`

---

## 5) Run the frontend (Angular)

Open a terminal in the `frontend` folder and run:

```bash
npm install
ng serve
```

Then open your browser at: **http://localhost:4200**

---

## 6) Register an account and try it out

Open `http://localhost:4200` — you'll land on the login page. Click **"Create one"**
to register (name, email, password — min 6 characters). This calls `POST /api/auth/register`,
which creates your `User` row and immediately logs you in by returning a JWT token.

By default, email sending is disabled - check your **IntelliJ console** right after
registering, you'll see the verification email's content (including the link) printed there
instead of arriving in an inbox. You can click/copy that link to test the flow immediately.

The dashboard will look a little empty at first — that's expected! Here's the order to add data:

1. Go to **Settings** → pick your currency, and enter your monthly salary, payment day, and
   monthly working hours (you can also link a salary wallet - do this after step 2).
2. Go to **Wallets** → create at least one wallet, e.g. `Bank Account`. Then go back to
   Settings and pick it as your salary wallet if you'd like automatic deposits.
3. Go to **Categories** → add a couple, e.g. `Restaurants` with a $300 monthly limit,
   `Marketing` with a $500 monthly limit - each one linked to a wallet.
4. Go to **Dream** → set a goal, e.g. `MacBook Pro` with a $2000 target.
5. Go to **Transactions** → log a few expenses. Watch your wallet balance drop by the same
   amount on the **Wallets** page. Try logging some right around your payday to see the
   payday-pattern insight appear.
6. Go back to **Dashboard** → now you'll see wallet balances, the daily title, twin comparison,
   pace warnings, dream damage/progress, memory lane, payday patterns, time cost, your Dream
   Garden, and your streak - all computed from your real data.

Everything you create is tied to your account — try registering a second account in an
incognito window and you'll see it starts completely empty.

---

## 🛡️ How the newest additions work (for learning)

### Bank CSV import (`CsvImportService`)
A free alternative to paid bank-linking services like Plaid (which start around $500+/month -
not compatible with a $0 budget). You export a CSV statement from your bank's website, upload
it on the Transactions page, and pick which category the whole batch belongs to.
`CsvImportService` looks for recognizable column names (Date/Description/Amount, case-insensitive,
several common variants) and tries a handful of common date formats - rows it can't confidently
parse are skipped with a specific reason rather than failing the entire import. Every imported
row still goes through the normal `Transaction` creation path, so it debits the right wallet
exactly like a manually-entered expense would.


### Multiple dreams
`DreamController` already supported creating many `Dream` rows - the change was in
`DashboardService.buildDreamProtector`, which used to grab only `dreams.get(0)`. It now loops
over every dream and builds a `DreamStatus` for each, so the same month's overspend/savings is
shown as a percentage of EVERY goal you're tracking, side by side.

### Recurring expenses (`RecurringTransaction`, `RecurringTransactionService`)
Same automatic pattern as the salary deposit: a `RecurringTransaction` is just a *rule*
("charge $15 to Subscriptions on day 5"). `RecurringTransactionService.generateDueTransactions()`
runs at the top of every dashboard load, checks each active rule, and creates a real
`Transaction` (debiting the right wallet) the first time that day arrives each month -
`lastGeneratedMonth` prevents duplicates if the dashboard loads twice in one day.

### Rate limiting (`RateLimiter`)
A simple in-memory counter (no Redis needed) tracks failed login/register attempts per
IP+email. After 5 failures within 15 minutes, further attempts get a 429 "Too Many Requests"
response. Being in-memory, it resets on server restart - a fine trade-off for a small app,
not for a large-scale production service.

### Pagination + CSV export
`GET /api/transactions` now takes `?page=&size=` and returns a `PagedResponse` instead of a
raw array - the Transactions page shows 20 at a time with a "Load more" button, so an account
with thousands of expenses doesn't try to render them all at once. `GET /api/transactions/export`
is separate and unpaginated on purpose - it always returns everything, formatted as CSV with
proper quote-escaping, so you can back up your data or hand it to an accountant.

### Password reset + email verification (`VerificationToken`, `EmailService`)
Both flows share one `VerificationToken` entity (a random UUID + expiry + purpose) - "forgot
password" tokens expire in 1 hour, "verify email" tokens in 24 hours, and each is deleted
after one use so a link can't be replayed. `EmailService` sends real email via SMTP if
`app.mail-enabled=true` is set, but **defaults to printing the email content (including the
link) to the console** - so you can test the entire flow immediately with zero email setup,
then wire up a free Gmail App Password later when you're ready for real emails.

---

## 🔒 A security fix made along the way
While adding `Wallet`, we noticed `Category`, `Dream`, `Garden`, `UserStreak`, and
`UserSettings` were all returning their full `owner` (`User`) object - including the BCrypt
password hash - inside API responses, because Jackson serializes entities by default unless
told not to. Every `owner` field across these entities now has `@JsonIgnore`. This is a good
reminder: whenever an entity has a relationship back to `User`, double-check what actually
gets serialized to JSON.

---

## 👛 How Wallets & Currency work (for learning)

### Currency
Kept intentionally simple: **one currency per user**, stored on `UserSettings.currency`, applied
everywhere via Angular's `currency` pipe (`{{ amount | currency:currency }}`). We deliberately
did NOT give each wallet its own currency - if a "Euro wallet" and a "Dollar wallet" both fed
into the same dashboard totals (this month's spending, pace analysis, etc.), those numbers would
add unlike units together, which is mathematically wrong without real-time exchange rate
conversion. If you want true multi-currency support later, look at per-wallet dashboards or
integrating a currency-conversion API before summing across wallets.

### Wallets (`Wallet` entity, `WalletService`)
Every `Category` links to a `Wallet`. `WalletService.debit()` and `.credit()` are the only two
places wallet balances change:
- **Debit** — `TransactionController.createTransaction` calls `walletService.debit(...)` right
  after saving the expense, pulling from the category's linked wallet.
- **Credit (undo)** — deleting a transaction calls `.credit(...)` to put the money back.
- **Credit (salary)** — `WalletService.checkAndCreditSalaryIfDue()` runs at the top of every
  `DashboardService.buildDashboard()` call (so it's core, not gated by subscription tier). It
  checks: is today on/after this month's payday, is a salary wallet configured, and have we
  NOT already credited this month (`UserSettings.lastSalaryCreditedMonth` guards against double
  deposits)? If all three are true, the salary gets added automatically. There's also a manual
  "I got paid" button (`WalletController.creditSalaryNow`) on the Wallets page for convenience,
  since waiting for the exact payday to test this would be slow.

---

## 🌱 How the 3 newest features work (for learning)

### 💰 Payday Patterns (`SalaryInsightService`)
Once you've entered your salary payment day in Settings, this service looks at your
transaction history and compares "the week right after payday" against "the rest of the
month" - both in overall spending pace and per-category concentration. It needs at least
5 transactions before it will show anything (not enough data otherwise). Everything is
recalculated live each time the dashboard loads - there's no separate insights table.

### ⏳ Time Cost of Purchases (`TimeCostService`)
`hourlyRate = monthlySalary / monthlyWorkingHours`, computed once you fill in Settings.
Every transaction gets a `timeCostHours` and a friendly sentence (`TransactionResponse` DTO) -
notice the backend now returns a DTO instead of the raw `Transaction` entity for exactly this
reason: we need to attach computed, per-user data that isn't stored in the database. The same
math also feeds a monthly summary sentence on the dashboard, and gets woven directly into the
existing Daily Title message.

### 🌱 Dream Garden (`GardenService`, `Garden` entity)
Unlike the other features, the garden's state IS persisted (one `Garden` row per user) so we
can compare "is this better or worse than last time?" and say things like "some leaves are
falling" instead of just reporting a flat number. The growth score (0-100) blends four signals,
each worth up to 25 points: staying within category budgets, this month's savings rate, dream
progress, and a "consecutive clean days" streak. The score maps to 5 stages (Seed → Sprout →
Young Tree → Flowering Tree → Beautiful Garden).

### 🔥 Streaks & Badges (`StreakService`, `UserStreak` entity)
The CURRENT streak (consecutive recent days spent under your average daily budget) is
recalculated live every time, same as most features here. But the LONGEST streak (personal
best) is persisted in `UserStreak` - otherwise the moment a streak breaks and resets to 0,
that record would be lost forever. Badges (`BadgeDto`) are computed live from current stats -
7-day and 30-day streaks, reaching 50% of your dream, and finishing a full calendar month
within budget.

---

## 🔐 How the login system works (for learning)

1. **Register/Login** (`AuthController`) — you send an email+password to `/api/auth/register`
   or `/api/auth/login`. The backend checks/hashes the password with **BCrypt** and, if valid,
   returns a **JWT token** (`JwtService`) — a signed string proving who you are.
2. **Storing the token** (`AuthService` in Angular) — the token is saved in the browser's
   `localStorage` so you stay logged in across page refreshes.
3. **Sending the token** (`auth.interceptor.ts`) — every subsequent API call from Angular
   automatically gets an `Authorization: Bearer <token>` header attached.
4. **Checking the token** (`JwtAuthFilter` in Spring) — on every request, this filter reads
   that header, verifies the token's signature, and tells Spring Security who's making the request.
5. **Scoping the data** (`CurrentUserProvider` + repository methods like `findByOwner`) —
   every controller now asks "who is this?" and only returns/modifies that user's own rows.
6. **Protecting pages** (`auth.guard.ts`) — if you try to visit `/transactions` without being
   logged in, Angular's router redirects you to `/login` before the page even loads.

---

## 💳 How the trial & 3-tier subscription system works (for learning)

1. **Trial starts at registration** — `User`'s constructor sets `trialStartDate = now` and
   `subscriptionStatus = TRIAL`, `planTier = FREE`.
2. **Computing the effective tier** — `SubscriptionService.getEffectiveTier(user)` is the single
   source of truth: during an active trial it always returns `PREMIUM` (full access), regardless
   of `planTier`. Once the trial ends, it returns whatever `planTier` they've actually paid for
   (defaulting to `FREE`). An `ACTIVE` subscription always wins and returns the tier they bought.
3. **Gating features** — `DashboardService.buildDashboard()` checks the tier once, then builds
   the Plus-level features (Daily Title, Twin Comparison message, Pace Analysis) only if
   `tier >= PLUS`, and the Premium-level features (Dream Protector, Memory Lane, Talking Reports,
   Payday Patterns, Time Cost, Dream Garden) only if `tier == PREMIUM`. Basic numbers stay visible
   on every tier.
4. **Choosing a plan** (`BillingComponent` + `SubscriptionController.createCheckoutSession`) —
   the user picks a tier and monthly/annual interval; the backend resolves the matching Stripe
   Price ID (one of 4) and creates a **Checkout Session**, embedding which tier was chosen in the
   session's `metadata` (Stripe has no built-in concept of "this price = Plus").
5. **Confirming payment** (`SubscriptionController.handleWebhook`) — Stripe calls our webhook
   directly after payment, we verify its signature, read the tier back out of `metadata`, and set
   `subscriptionStatus = ACTIVE` + `planTier` accordingly.

This webhook step is why local testing needs the Stripe CLI (`stripe listen --forward-to ...`) -
Stripe's servers can't reach `localhost` directly, so the CLI creates a tunnel for testing.

---

## 🧠 How the code is organized (for learning)

### Backend (`backend/src/main/java/com/budgetbutler`)

| Folder | What it does |
|---|---|
| `model/` | `User`, `UserSettings`, `Category`, `Transaction`, `Dream`, `Garden`, `UserStreak`, `Wallet`, plus `SubscriptionStatus`/`PlanTier`/`BillingInterval` enums |
| `repository/` | Interfaces that give us free database queries (Spring Data JPA) |
| `controller/` | REST endpoints - `AuthController`, `SubscriptionController`, `UserSettingsController`, `GardenController`, `StreakController`, `WalletController`, plus the core CRUD controllers |
| `security/` | `JwtService`, `JwtAuthFilter`, `SecurityConfig`, `CustomUserDetailsService`, `CurrentUserProvider` |
| `service/DashboardService.java` | Orchestrates every "personality" feature into one response |
| `service/SubscriptionService.java` | "Is this user's premium access active right now?" |
| `service/SalaryInsightService.java` | Feature: Payday Patterns |
| `service/TimeCostService.java` | Feature: Time Cost of Purchases |
| `service/GardenService.java` | Feature: Dream Garden |
| `service/StreakService.java` | Feature: Streaks & Badges |
| `service/WalletService.java` | Core: debiting/crediting wallets, automatic salary deposits |
| `dto/` | JSON shapes sent to/from Angular, including `TransactionResponse` (adds computed time cost), `GardenResponse`, `UserSettingsResponse` |
| `config/CorsConfig.java`, `config/StripeConfig.java` | Lets Angular call the API, initializes Stripe on startup |

Start reading at `DashboardService.java` to see how all the features come together, or dive
into any single `service/*.java` file for one feature at a time - each is self-contained.

### Frontend (`frontend/src/app`)

| Folder | What it does |
|---|---|
| `models/models.ts` | TypeScript "shapes" matching the backend JSON |
| `services/api.service.ts` | Calls the data endpoints (categories, transactions, dreams, dashboard) |
| `services/auth.service.ts` | Calls login/register, stores the token, tracks logged-in state |
| `interceptors/auth.interceptor.ts` | Attaches the token to every outgoing request automatically |
| `guards/auth.guard.ts` | Blocks access to pages if you're not logged in |
| `auth/login/`, `auth/register/` | The login and registration forms |
| `billing/` | The subscription page - shows trial countdown / subscribe button |
| `settings/` | Onboarding form for salary, payday, working hours, currency, and salary wallet |
| `wallets/` | Create/delete wallets, see balances, manual "I got paid" button |
| `services/subscription.service.ts` | Checks trial/subscription status and starts the Stripe checkout flow |
| `services/settings.service.ts` | Get/save salary and work-hours settings |
| `dashboard/` | The main dashboard page - now also renders Payday Patterns, Time Cost, and Dream Garden |
| `categories/`, `transactions/`, `dreams/` | Simple forms + tables to manage your data |
| `app.routes.ts` | Maps URLs (`/login`, `/transactions`, `/dreams`...) to components, with guards on the protected ones |

Each "page" is a **standalone component** — no `NgModule` needed. Each one has 3 files:
`.ts` (logic), `.html` (template), `.css` (styling).

---

## 🚀 Ideas for what to build next

- Persist the Dream Protector's monthly "saved amount" back to the database (currently it's
  calculated live each time, not saved)
- Add "forgot password" / email verification flows
- Turn the weekly/monthly "Talking Reports" into real push notifications using `@Scheduled`
- Add charts (e.g. with Chart.js) to visualize spending by category
- Support multiple dreams instead of just one active dream
- Add role-based access (e.g. an admin view) using Spring Security's authorities

Take it one feature at a time — the codebase is intentionally small so you can extend
any single file without getting lost.
