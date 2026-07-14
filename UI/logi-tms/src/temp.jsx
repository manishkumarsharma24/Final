import React, { useState } from 'react';
import { 
  Menu, Bell, User, Search, Home, 
  BarChart2, ShieldAlert, Settings, ChevronRight 
} from 'lucide-react';

export default function App() {
  // 1. Structural State Layout Controls (Collapses/Expands your Sidebar menu)
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  // 2. Local Array State for your Operational Data Table Manifest
  const [tableData] = useState([
    { id: "TRK-0192", vehicle: "Volvo FH16", route: "Route A", driver: "John Doe", status: "Active" },
    { id: "TRK-0841", vehicle: "Scania R500", route: "Route B", driver: "Jane Smith", status: "Delayed" },
    { id: "TRK-0311", vehicle: "MAN TGX", route: "Route C", driver: "Bob Johnson", status: "In Depot" },
  ]);

  return (
    // MAIN APP WRAPPER: vh-100 locks view to screen height. overflow-hidden prevents global scrollbars.
    <div className="d-flex flex-column vh-100 vw-100 bg-light overflow-hidden">
      
      {/* ================================================================= */}
      {/* 1. NAVBAR COMPONENT CONTAINER                                     */}
      {/* ================================================================= */}
      <header className="navbar bg-white border-bottom shadow-sm px-4 justify-content-between flex-shrink-0" style={{ height: '64px', zIndex: 30 }}>
        
        {/* [Logo] & [Menu Toggle] Button */}
        <div className="d-flex align-items-center gap-3">
          <div className="d-flex align-items-center gap-2">
            <div className="bg-primary text-white rounded d-flex align-items-center justify-content-center fw-bold" style={{ width: '32px', height: '32px' }}>
              T
            </div>
            <strong className="fs-5 text-dark d-none d-sm-block">LogiTMS</strong>
          </div>
          <button 
            onClick={() => setIsSidebarOpen(!isSidebarOpen)}
            className="btn btn-light text-secondary d-flex align-items-center justify-content-center p-2 border-0"
            aria-label="Toggle Menu Drawer"
          >
            <Menu size={20} />
          </button>
        </div>

        {/* [Search Bar] Input Field Panel */}
        <div className="flex-grow-1 mx-4 d-none d-md-block" style={{ maxWidth: '400px' }}>
          <div className="position-relative">
            <Search size={16} className="text-muted position-absolute start-0 top-50 translate-middle-y ms-3" />
            <input 
              type="text" 
              placeholder="Search manifests, vehicles, destinations..." 
              className="form-control form-control-sm bg-light ps-5 py-2 border-light-subtle rounded-3"
            />
          </div>
        </div>

        {/* [Alerts] Notifications Indicator & [Profile] Accounts Block Dropdown */}
        <div className="d-flex align-items-center gap-2">
          <button className="btn btn-light position-relative text-secondary p-2 border-0">
            <Bell size={20} />
            <span className="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-white rounded-circle"></span>
          </button>
          <div className="vr mx-2 text-black-50" style={{ height: '24px' }}></div>
          <div className="d-flex align-items-center gap-2 bg-light p-1 pe-3 rounded-pill">
            <div className="bg-primary-subtle text-primary rounded-circle d-flex align-items-center justify-content-center" style={{ width: '32px', height: '32px' }}>
              <User size={16} />
            </div>
            <span className="small fw-semibold text-secondary d-none d-lg-block">Admin Account</span>
          </div>
        </div>
      </header>

      {/* LOWER CONTAINER WORKSPACE (Splits space horizontally into Sidebar + Dashboard elements) */}
      <div className="d-flex flex-grow-1 w-100 overflow-hidden position-relative">
        
        {/* ================================================================= */}
        {/* 2. SIDEBAR NAVIGATION DRAWER WIDGETS                              */}
        {/* ================================================================= */}
        <aside 
          className="bg-dark text-light border-end border-secondary shadow-lg flex-shrink-0"
          style={{ 
            width: isSidebarOpen ? '260px' : '0px', 
            transition: 'width 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
            overflow: 'hidden'
          }}
        >
          <div className="p-4 h-100 d-flex flex-column" style={{ width: '260px' }}>
            <p className="text-uppercase text-muted small fw-bold tracking-wider mb-4 px-2">Operations Center</p>
            <nav className="nav flex-column gap-1 flex-grow-1">
              <a href="#" className="nav-link active bg-primary text-white rounded-3 d-flex align-items-center gap-3 px-3 py-2.5">
                <Home size={16} />
                <span>Dashboard</span>
              </a>
              <a href="#" className="nav-link text-light rounded-3 d-flex align-items-center gap-3 px-3 py-2.5 opacity-75">
                <BarChart2 size={16} />
                <span>Analytics</span>
              </a>
              <a href="#" className="nav-link text-light rounded-3 d-flex align-items-center gap-3 px-3 py-2.5 opacity-75">
                <ShieldAlert size={16} />
                <span>Management</span>
              </a>
              <a href="#" className="nav-link text-light rounded-3 d-flex align-items-center gap-3 px-3 py-2.5 opacity-75">
                <Settings size={16} />
                <span>Settings</span>
              </a>
            </nav>
            <div className="pt-3 border-top border-secondary text-muted small px-2">
              v1.0.0 Production Local
            </div>
          </div>
        </aside>

        {/* ================================================================= */}
        {/* 3. MAIN WORKSPACE VIEWPORTS                                      */}
        {/* ================================================================= */}
        <div className="flex-grow-1 overflow-auto bg-light">
          <div className="p-4 mx-auto" style={{ maxWidth: '1400px' }}>
            
            {/* 3A. BREADCRUMB INDICATOR ROW */}
            <nav className="d-flex align-items-center gap-2 mb-4 small text-muted">
              <a href="#" className="text-decoration-none text-primary">Home</a>
              <ChevronRight size={12} className="text-black-50" />
              <span className="fw-bold text-dark">Dashboard</span>
            </nav>

            {/* 3B. MAIN CONTENT PANELS (Three column metrics) */}
            <section className="row g-4 mb-4" aria-label="Operational Summary Metrics">
              {/* KPI Card 1 */}
              <div className="col-12 col-md-6 col-lg-4">
                <div className="card h-100 border-light-subtle shadow-sm p-4 bg-white rounded-3">
                  <span className="text-uppercase text-muted small tracking-wider fw-bold">Fleet Operational Utilization</span>
                  <div className="d-flex align-items-baseline gap-2 mt-2">
                    <h3 className="mb-0 fw-bold text-dark">84.2%</h3>
                    <span className="badge bg-success-subtle text-success small">+2.4%</span>
                  </div>
                </div>
              </div>
              {/* KPI Card 2 */}
              <div className="col-12 col-md-6 col-lg-4">
                <div className="card h-100 border-light-subtle shadow-sm p-4 bg-white rounded-3">
                  <span className="text-uppercase text-muted small tracking-wider fw-bold">Active Dispatched Loads</span>
                  <div className="d-flex align-items-baseline gap-2 mt-2">
                    <h3 className="mb-0 fw-bold text-dark">142</h3>
                    <span className="badge bg-warning-subtle text-warning small">Running</span>
                  </div>
                </div>
              </div>
              {/* KPI Card 3 */}
              <div className="col-12 col-md-12 col-lg-4">
                <div className="card h-100 border-light-subtle shadow-sm p-4 bg-white rounded-3">
                  <span className="text-uppercase text-muted small tracking-wider fw-bold">Critical Safety Alerts</span>
                  <div className="d-flex align-items-baseline gap-2 mt-2">
                    <h3 className="mb-0 fw-bold text-danger">3</h3>
                    <span className="badge bg-danger-subtle text-danger small">Action Needed</span>
                  </div>
                </div>
              </div>
            </section>

            {/* 3C. DATA TABLE CONTAINER CARD ELEMENT */}
            <section className="card border-light-subtle shadow-sm rounded-3 overflow-hidden bg-white">
              <div className="card-header bg-light bg-opacity-50 border-bottom d-flex align-items-center justify-content-between p-3">
                <h6 className="mb-0 fw-bold text-dark">Active Tracking Manifest Matrix</h6>
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
                    {tableData.map((row) => (
                      <tr key={row.id}>
                        <td className="py-3 px-4 fw-bold text-primary">{row.id}</td>
