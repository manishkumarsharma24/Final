import React, { useState } from "react";
import Navbar from "./components/Navbar";
import Sidebar from "./components/Sidebar";
import DashboardContent from "./components/DashboardContent";

export default function App() {
  // Main centralized state management
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  const [tableData] = useState([
    {
      id: "TRK-0192",
      vehicle: "Volvo FH16",
      route: "Route A",
      driver: "John Doe",
      status: "Active",
    },
    {
      id: "TRK-0841",
      vehicle: "Scania R500",
      route: "Route B",
      driver: "Jane Smith",
      status: "Delayed",
    },
    {
      id: "TRK-0311",
      vehicle: "MAN TGX",
      route: "Route C",
      driver: "Bob Johnson",
      status: "In Depot",
    },
  ]);

  return (
    <div className="d-flex flex-column vh-100 vw-100 bg-light overflow-hidden">
      {/* Top Navbar */}
      <Navbar
        isSidebarOpen={isSidebarOpen}
        setIsSidebarOpen={setIsSidebarOpen}
      />

      {/* Main App Workspace layout split container */}
      <div className="d-flex flex-grow-1 w-100 overflow-hidden position-relative">
        <Sidebar isOpen={isSidebarOpen} />
        <DashboardContent data={tableData} />
      </div>
    </div>
  );
}
