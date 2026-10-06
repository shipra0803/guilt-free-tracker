// The three top-level app tabs, in display order.
const TABS = [
  { key: "home", label: "Home" },
  { key: "history", label: "History" },
  { key: "settings", label: "Settings" },
];

/** Simple in-app tab switcher - no router, just view-state in App.jsx. */
export default function Nav({ active, onChange }) {
  return (
    <nav className="tab-nav" role="tablist" aria-label="Pages">
      {TABS.map((tab) => (
        <button
          key={tab.key}
          type="button"
          role="tab"
          aria-selected={active === tab.key}
          className={`tab-nav-item ${active === tab.key ? "is-active" : ""}`}
          onClick={() => onChange(tab.key)}
        >
          {tab.label}
        </button>
      ))}
    </nav>
  );
}
