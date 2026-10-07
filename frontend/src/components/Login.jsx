import { useEffect, useState } from "react";
import { api } from "../api";

// Login page. On the very first visit (no account yet) it's a sign-up form instead: create the
// account, then log straight in. Either way App checks the login by loading the budget - the
// backend answers 401 if it's wrong, and App shows that as `error`.
export default function Login({ error, onLogin }) {
  const [signupOpen, setSignupOpen] = useState(null); // null while asking the server
  const [signupError, setSignupError] = useState(null);

  useEffect(() => {
    api
      .getSignupStatus()
      .then((status) => setSignupOpen(status.open))
      .catch(() => setSignupOpen(false)); // can't tell - fall back to the normal login form
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const username = data.get("username").trim();
    const password = data.get("password");

    if (signupOpen) {
      if (password.length < 8) {
        setSignupError("Use a password of at least 8 characters.");
        return;
      }
      if (password !== data.get("confirm")) {
        setSignupError("The two passwords don't match.");
        return;
      }
      try {
        await api.signUp(username, password);
      } catch (err) {
        console.error(err);
        // 409 = someone already created the account; show the normal login form.
        if (err.status === 409) setSignupOpen(false);
        setSignupError(err.status === 409 ? "An account already exists. Log in instead." : "Couldn't create the account. Try again.");
        return;
      }
    }
    onLogin(username, password);
  }

  if (signupOpen === null) return <div className="app-shell app-shell-center" aria-busy="true" />;

  return (
    <div className="app-shell app-shell-center">
      <form className="card login-card" onSubmit={handleSubmit}>
        <div className="wordmark" aria-hidden="true">guilt-free.</div>
        <h1 className="login-title">{signupOpen ? "Create your account" : "Log in"}</h1>
        {signupOpen && (
          <p className="budget-setup-hint">
            There's only one account and no password reset, so pick something you'll remember.
          </p>
        )}
        <div className="field">
          <label htmlFor="username">Username</label>
          <input id="username" name="username" type="text" autoComplete="username" required maxLength={50} autoFocus />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete={signupOpen ? "new-password" : "current-password"}
            required
            minLength={signupOpen ? 8 : undefined}
            maxLength={72}
          />
        </div>
        {signupOpen && (
          <div className="field">
            <label htmlFor="confirm">Confirm password</label>
            <input id="confirm" name="confirm" type="password" autoComplete="new-password" required />
          </div>
        )}
        {(signupError || error) && <p className="field-error" role="alert">{signupError || error}</p>}
        <button type="submit" className="btn-primary">
          {signupOpen ? "Create account" : "Log in"}
        </button>
      </form>
    </div>
  );
}
