import './App.css'
import RegisterForm from "./components/RegisterForm.jsx";
import LoginForm from "./components/LoginForm.jsx";
import Navbar from "./components/Navbar.jsx";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import DashboardLayout from "./components/DashboardLayout/DashboardLayout.jsx";
import {Home} from "./pages/Home/Home.jsx";
import { AuthProvider, AuthContext } from "./context/AuthContext.jsx";
import { useContext } from "react";
import {ProfileSettings} from "./components/ProfileSettings/ProfileSettings.jsx";

// Protected Route component
const ProtectedRoute = ({ element }) => {
  const { user } = useContext(AuthContext);

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return element;
};

// Auth routes (accessible only when NOT logged in)
const AuthRoute = ({ element }) => {
  const { user } = useContext(AuthContext);

  if (user) {
    return <Navigate to="/" replace />;
  }

  return element;
};

function AppRoutes() {
  const { user } = useContext(AuthContext);

  return (
    <BrowserRouter>
      {user && <Navbar/>}
      <Routes>
        {/* Auth pages - only accessible when not logged in */}
        <Route path="/login" element={<AuthRoute element={<LoginForm />} />} />
        <Route path="/register" element={<AuthRoute element={<RegisterForm />} />} />

        {/* Dashboard - only accessible when logged in */}
        <Route path="/" element={<ProtectedRoute element={<DashboardLayout />} />}>
          <Route index element={<Navigate to="/home" replace />} />
          <Route path="home" element={<Home />} />
          <Route path="profile-settings" element={<ProfileSettings />} />
        </Route>

        {/* Default redirect */}
        <Route path="*" element={<Navigate to={user ? "/" : "/login"} replace />} />
      </Routes>
    </BrowserRouter>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}

export default App
