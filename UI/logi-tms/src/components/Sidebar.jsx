import React from "react";
import { Home, BarChart2, ShieldAlert, Settings } from "lucide-react";

export default function Sidebar({ isOpen }) {
  return (
    <aside
      className="bg-dark text-light border-end border-secondary shadow-lg flex-shrink-0"
      style={{
        width: isOpen ? "260px" : "0px",
        transition: "width 0.25s cubic-bezier(0.4, 0, 0.2, 1)",
        overflow: "hidden",
      }}
    >
      <div className="p-4 h-100 d-flex flex-column" style={{ width: "260px" }}>
        <p className="text-uppercase text-muted small fw-bold tracking-wider mb-4 px-2">
          Operations Center
        </p>
        <nav className="nav flex-column gap-1 flex-grow-1">
          <a
            href="#"
            className="nav-link active bg-primary text-white rounded-3 d-flex align-items-center gap-3 px-3 py-2.5"
          >
            <Home size={16} />
            <span>Dashboard</span>
          </a>
          <a
            href="#"
            className="nav-link text-light rounded-3 d-flex align-items-center gap-3 px-3 py-2.5 opacity-75"
          >
            <BarChart2 size={16} />
            <span>Analytics</span>
          </a>
          <a
            href="#"
            className="nav-link text-light rounded-3 d-flex align-items-center gap-3 px-3 py-2.5 opacity-75"
          >
            <ShieldAlert size={16} />
            <span>Management</span>
          </a>
          <a
            href="#"
            className="nav-link text-light rounded-3 d-flex align-items-center gap-3 px-3 py-2.5 opacity-75"
          >
            <Settings size={16} />
            <span>Settings</span>
          </a>
        </nav>
        <div className="pt-3 border-top border-secondary text-muted small px-2">
          v1.0.0 Production Local
        </div>
      </div>
    </aside>
  );
}
