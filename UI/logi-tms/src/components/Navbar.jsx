import React from "react";
import { Menu, Bell, User, Search } from "lucide-react";

export default function Navbar({ isSidebarOpen, setIsSidebarOpen }) {
  return (
    <header
      className="navbar bg-white border-bottom shadow-sm px-4 justify-content-between flex-shrink-0"
      style={{ height: "64px", zIndex: 30 }}
    >
      {/* Brand Identity & Menu Trigger */}
      <div className="d-flex align-items-center gap-3">
        <div className="d-flex align-items-center gap-2">
          <div
            className="bg-primary text-white rounded d-flex align-items-center justify-content-center fw-bold"
            style={{ width: "32px", height: "32px" }}
          >
            T
          </div>
          <strong className="fs-5 text-dark d-none d-sm-block">LogiTMS</strong>
        </div>
        <button
          onClick={() => setIsSidebarOpen(!isSidebarOpen)}
          className="btn btn-light text-secondary d-flex align-items-center justify-content-center p-2 border-0"
          aria-label="Toggle Navigation Sidebar"
        >
          <Menu size={20} />
        </button>
      </div>

      {/* Global Search Input Box */}
      <div
        className="flex-grow-1 mx-4 d-none d-md-block"
        style={{ maxWidth: "400px" }}
      >
        <div className="position-relative">
          <Search
            size={16}
            className="text-muted position-absolute start-0 top-50 translate-middle-y ms-3"
          />
          <input
            type="text"
            placeholder="Search manifests, vehicles, destinations..."
            className="form-control form-control-sm bg-light ps-5 py-2 border-light-subtle rounded-3"
          />
        </div>
      </div>

      {/* Alerts and Account Profiles */}
      <div className="d-flex align-items-center gap-2">
        <button className="btn btn-light position-relative text-secondary p-2 border-0">
          <Bell size={20} />
          <span className="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-white rounded-circle"></span>
        </button>
        <div className="vr mx-2 text-black-50" style={{ height: "24px" }}></div>
        <div className="d-flex align-items-center gap-2 bg-light p-1 pe-3 rounded-pill">
          <div
            className="bg-primary-subtle text-primary rounded-circle d-flex align-items-center justify-content-center"
            style={{ width: "32px", height: "32px" }}
          >
            <User size={16} />
          </div>
          <span className="small fw-semibold text-secondary d-none d-lg-block">
            Admin Account
          </span>
        </div>
      </div>
    </header>
  );
}
