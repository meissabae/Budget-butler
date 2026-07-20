# 🚀 Deploying Budget Butler for $0

This guide gets your app live on the internet using entirely free tiers. No credit card
required for any of these three services (as of July 2026).

**The stack:**
| Piece | Service | Why |
|---|---|---|
| Frontend (Angular) | **Netlify** | Simplest free static hosting, generous free tier, no card needed |
| Backend (Spring Boot) | **Render** | Free web service tier, deploys straight from GitHub, no Docker required |
| Database (MySQL) | **Aiven** | Genuinely free-forever MySQL (1GB RAM/storage), no card, no time limit |

⚠️ Free tiers have real trade-offs: Render's free web services **spin down after 15 minutes
of inactivity** and take ~30 seconds to "wake up" on the next request. That's fine for a
personal project or demo, not for a production app with real users expecting instant responses.

---

## 1) Push your code to GitHub

Both Netlify and Render deploy by connecting to a GitHub repository.

```bash
cd budget-butler
git init
git add .
git commit -m "Initial commit"
```

Create a new repository on [github.com](https://github.com/new), then:

```bash
git remote add origin https://github.com/YOUR_USERNAME/budget-butler.git
git branch -M main
git push -u origin main
```

**Important:** Before pushing, double check `application.properties` doesn't contain your
real Stripe secret key or a real database password if this repo will be public - use
placeholder values in the file and set the real ones as environment variables in Render
(step 3) instead.

---

## 2) Set up the free MySQL database (Aiven)

1. Go to [aiven.io](https://aiven.io) → sign up (no card required).
2. Create a new service → choose **MySQL** → select the **Free plan**.
3. Once it's running, go to the service's **Overview** tab and copy:
   - **Host**
   - **Port**
   - **User** (usually `avnadmin`)
   - **Password**
   - **Default database name**

You'll plug these into Render in the next step.

---

## 3) Deploy the backend (Render)

1. Go to [render.com](https://render.com) → sign up with GitHub (no card required).
2. **New → Web Service** → connect your `budget-butler` repository.
3. Configure:
   - **Root Directory:** `backend`
   - **Language:** **Docker** (Render does NOT run Java natively - only Node.js, Python,
     Ruby, Go, Rust, and Elixir have native runtimes. Java requires a Dockerfile, which is
     already included at `backend/Dockerfile`, so Render will build and run it automatically
     once you pick "Docker" here.)
   - **Instance Type:** Free

   You don't need to fill in Build/Start commands - Render reads them from the Dockerfile.
   The Dockerfile also caps JVM memory (`-Xmx400m`) to fit inside the free tier's 512MB limit.

4. Add these **Environment Variables** (Render's dashboard → Environment tab). Spring Boot
   automatically maps environment variables to properties, so you don't need to edit any code:

   | Key | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:mysql://<aiven-host>:<aiven-port>/<db-name>?useSSL=true&serverTimezone=UTC` |
   | `SPRING_DATASOURCE_USERNAME` | your Aiven username |
   | `SPRING_DATASOURCE_PASSWORD` | your Aiven password |
   | `JWT_SECRET` | a long random string (generate one at [random.org](https://www.random.org/strings/) or just mash your keyboard for 40+ characters) |
   | `STRIPE_SECRET_KEY` | your Stripe secret key (or leave the placeholder if skipping Stripe for now) |
   | `STRIPE_PRICE_ID_PLUS_MONTHLY` etc. | your 4 Stripe price IDs |
   | `STRIPE_WEBHOOK_SECRET` | from the Stripe CLI or dashboard webhook setup |
   | `APP_FRONTEND_URL` | your Netlify URL (you'll get this in step 4 - come back and set this after) |
   | `CORS_ALLOWED_ORIGIN` | same as above - your Netlify URL |

5. Click **Deploy**. Render gives you a URL like `https://budget-butler-backend.onrender.com`
   - copy it, you'll need it next.

---

## 4) Deploy the frontend (Netlify)

1. Before deploying, point Angular at your live backend instead of `localhost`. Edit
   `frontend/src/environments/environment.ts`:
   ```ts
   export const environment = {
     apiUrl: 'https://budget-butler-backend.onrender.com/api'  // your Render URL from step 3
   };
   ```
   Commit and push this change.

2. Go to [netlify.com](https://netlify.com) → sign up with GitHub (no card required).
3. **Add new site → Import an existing project** → pick your repository.
4. Configure:
   - **Base directory:** `frontend`
   - **Build command:** `npm install && npx ng build`
   - **Publish directory:** `frontend/dist/budget-butler-frontend/browser`
5. Click **Deploy**. Netlify gives you a URL like `https://budget-butler.netlify.app`.

---

## 5) Connect the two (final step)

Now that you have your real Netlify URL, go back to **Render → Environment** and update:
- `APP_FRONTEND_URL` → `https://budget-butler.netlify.app`
- `CORS_ALLOWED_ORIGIN` → `https://budget-butler.netlify.app`

Render will automatically redeploy with the new values. Once that finishes, open your Netlify
URL - you should be able to register an account and use the app live on the internet.

---

## 6) (Optional) Point Stripe's webhook at your live backend

If you set up Stripe: in the Stripe Dashboard → **Developers → Webhooks → Add endpoint**, use:
```
https://budget-butler-backend.onrender.com/api/subscription/webhook
```
Select the `checkout.session.completed` event. Copy the new signing secret it gives you and
update `STRIPE_WEBHOOK_SECRET` in Render (you no longer need the Stripe CLI once this is set -
that was only for local testing).

---

## Known limitations of this $0 setup

- **Cold starts:** Render's free tier sleeps after 15 minutes idle; the first request after
  that takes ~30 seconds while it wakes up. Fine for personal use/demos.
- **No custom domain SSL complexity:** both Netlify and Render give you free HTTPS out of the
  box on their `.netlify.app` / `.onrender.com` subdomains - no extra setup needed.
- **Database size:** Aiven's free MySQL caps at 1GB - more than enough for personal use, but
  keep an eye on it if you invite lots of test users.
- **This is a demo-grade setup**, not a production launch checklist - before real users and
  real money flow through it, you'd want proper secret management, database backups, and a
  paid tier that doesn't sleep.

If you outgrow the free tiers later, Render's paid web services start around $7/month and
remove the cold-start issue, which is usually the first upgrade worth paying for.
