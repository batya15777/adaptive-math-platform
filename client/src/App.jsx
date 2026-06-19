import './App.css'
import RegisterForm from "./components/RegisterForm.jsx";
import LoginForm from "./components/LoginForm.jsx";
import Navbar from "./components/Navbar.jsx";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import DashboardLayout from "./components/DashboardLayout/DashboardLayout.jsx";
import {Home} from "./pages/Home/Home.jsx";
import { AuthProvider } from "./context/AuthContext.jsx";
import { AuthContext } from "./context/AuthContextSetup.js";
import { ProfileProvider } from "./contexts/ProfileContext.jsx";
import { useContext } from "react";
import {ProfileSettings} from "./components/ProfileSettings/ProfileSettings.jsx";
import { MathTraining } from "./pages/MathTraining/MathTraining.jsx";
import { QuestionGame } from "./pages/QuestionGame/QuestionGame.jsx";
import { LevelManagerPage } from "./pages/LevelManager/LevelManagerPage.jsx";

// Waits for session restore before deciding — prevents redirect on refresh
const ProtectedRoute = ({ element }) => {
  const { user, authLoading } = useContext(AuthContext);
  if (authLoading) return null;
  if (!user) return <Navigate to="/login" replace />;
  return element;
};

// Auth routes (login/register) — wait so a refreshed logged-in user isn't flashed the login page
const AuthRoute = ({ element }) => {
  const { user, authLoading } = useContext(AuthContext);
  if (authLoading) return null;
  if (user) return <Navigate to="/" replace />;
  return element;
};

function AppRoutes() {
  const { user, authLoading } = useContext(AuthContext);

  return (
    <BrowserRouter>
      {!authLoading && user && <Navbar/>}
      <Routes>
        {/* Auth pages - only accessible when not logged in */}
        <Route path="/login" element={<AuthRoute element={<LoginForm />} />} />
        <Route path="/register" element={<AuthRoute element={<RegisterForm />} />} />

        {/* Level survey — protected but outside DashboardLayout to avoid the survey gate loop */}
        <Route path="/level-survey" element={<ProtectedRoute element={<LevelManagerPage />} />} />

        {/* Dashboard - only accessible when logged in */}
        <Route path="/" element={<ProtectedRoute element={<DashboardLayout />} />}>
          <Route index element={<Navigate to="/home" replace />} />
          <Route path="home" element={<Home />} />
          <Route path="math-training" element={<MathTraining />} />
          <Route path="math-training/:subSubjectId/play" element={<QuestionGame />} />
          <Route path="profile-settings" element={<ProfileSettings />} />
        </Route>

        {/* Default redirect */}
        <Route path="*" element={<Navigate to={!authLoading && user ? "/" : "/login"} replace />} />
      </Routes>
    </BrowserRouter>
  );
}

function App() {
  return (
    <AuthProvider>
      <ProfileProvider>
        <AppRoutes />
      </ProfileProvider>
    </AuthProvider>
  );
}

export default App
