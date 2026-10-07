import { useState } from "react";

// Busy/error state for an async UI action. run(fn, failMessage) resolves true on success,
// or shows failMessage and resolves false.
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  async function run(fn, failMessage) {
    setBusy(true);
    setError(null);
    try {
      await fn();
      return true;
    } catch (err) {
      console.error(err);
      setError(failMessage);
      return false;
    } finally {
      setBusy(false);
    }
  }

  return [busy, error, run];
}
