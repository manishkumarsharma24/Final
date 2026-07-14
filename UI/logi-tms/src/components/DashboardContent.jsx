import React from "react";
import { ChevronRight } from "lucide-react";

export default function DashboardContent({ data }) {
  return (
    <div className="flex-grow-1 overflow-auto bg-light">
      <div className="p-4 mx-auto" style={{ maxWidth: "1400px" }}>
        {/* Breadcrumb row tracking position */}
        <nav className="d-flex align-items-center gap-2 mb-4 small text-muted">
          <a href="#" className="text-decoration-none text-primary">
            Home
          </a>
          <ChevronRight size={12} className="text-black-50" />
          <span className="fw-bold text-dark">Dashboard</span>
        </nav>

        {/* Operational Statistics Summaries */}
        <section
          className="row g-4 mb-4"
          aria-label="Operational Summary Metrics"
        >
          <div className="col-12 col-md-6 col-lg-4">
            <div className="card h-100 border-light-subtle shadow-sm p-4 bg-white rounded-3">
              <span className="text-uppercase text-muted small tracking-wider fw-bold">
                Fleet Operational Utilization
              </span>
              <div className="d-flex align-items-baseline gap-2 mt-2">
                <h3 className="mb-0 fw-bold text-dark">84.2%</h3>
                <span className="badge bg-success-subtle text-success small">
                  +2.4%
                </span>
              </div>
            </div>
          </div>
          <div className="col-12 col-md-6 col-lg-4">
            <div className="card h-100 border-light-subtle shadow-sm p-4 bg-white rounded-3">
              <span className="text-uppercase text-muted small tracking-wider fw-bold">
                Active Dispatched Loads
              </span>
              <div className="d-flex align-items-baseline gap-2 mt-2">
                <h3 className="mb-0 fw-bold text-dark">142</h3>
                <span className="badge bg-warning-subtle text-warning small">
                  Running
                </span>
              </div>
            </div>
          </div>
          <div className="col-12 col-md-12 col-lg-4">
            <div className="card h-100 border-light-subtle shadow-sm p-4 bg-white rounded-3">
              <span className="text-uppercase text-muted small tracking-wider fw-bold">
                Critical Safety Alerts
              </span>
              <div className="d-flex align-items-baseline gap-2 mt-2">
                <h3 className="mb-0 fw-bold text-danger">3</h3>
                <span className="badge bg-danger-subtle text-danger small">
                  Action Needed
                </span>
              </div>
            </div>
          </div>
        </section>

        {/* Dynamic Tracking Manifest Grid Table */}
        <section className="card border-light-subtle shadow-sm rounded-3 overflow-hidden bg-white">
          <div className="card-header bg-light bg-opacity-50 border-bottom p-3">
            <h6 className="mb-0 fw-bold text-dark">
              Active Tracking Manifest Matrix
            </h6>
          </div>
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0 text-nowrap">
              <thead className="table-light text-uppercase small tracking-wider">
                <tr>
                  <th className="py-3 px-4 text-muted">ID Ref</th>
                  <th className="py-3 px-4 text-muted">Vehicle Type</th>
                  <th className="py-3 px-4 text-muted">Assigned Vector</th>
                  <th className="py-3 px-4 text-muted">Driver Name</th>
                  <th className="py-3 px-4 text-end text-muted">Status Flag</th>
                </tr>
              </thead>
              <tbody className="small">
                {data.map((row) => (
                  <tr key={row.id}>
                    <td className="py-3 px-4 fw-bold text-primary">{row.id}</td>
                    <td className="py-3 px-4 fw-bold text-dark">
                      {row.vehicle}
                    </td>
                    <td className="py-3 px-4 text-secondary">{row.route}</td>
                    <td className="py-3 px-4 text-secondary">{row.driver}</td>
                    <td className="py-3 px-4 text-end">
                      <span
                        className={`badge px-2.5 py-1.5 rounded-pill ${
                          row.status === "Active"
                            ? "bg-success-subtle text-success"
                            : row.status === "Delayed"
                              ? "bg-warning-subtle text-warning"
                              : "bg-secondary-subtle text-secondary"
                        }`}
                      >
                        {row.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}
