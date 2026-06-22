import './App.css'
import RegisterForm from "./components/auth/RegisterForm.jsx";
import LoginForm from "./components/auth/LoginForm.jsx";
import { Landing } from "./components/auth/Landing.jsx";
import ForgotPassword from "./components/auth/ForgotPassword.jsx";
import Navbar from "./components/Navbar.jsx";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import DashboardLayout from "./components/DashboardLayout/DashboardLayout.jsx";
import {Home} from "./pages/Home/Home.jsx";
import { AuthProvider } from "./context/AuthContext.jsx";
import { AuthContext } from "./context/AuthContextSetup.js";
import { ProfileProvider } from "./contexts/ProfileContext.jsx";
import { useContext, lazy, Suspense } from "react";
import {ProfileSettings} from "./components/ProfileSettings/ProfileSettings.jsx";
import { MathTraining } from "./pages/MathTraining/MathTraining.jsx";
import { QuestionGame } from "./pages/QuestionGame/QuestionGame.jsx";
import { LevelManagerPage } from "./pages/LevelManager/LevelManagerPage.jsx";
import { LeaderboardPage } from "./pages/Leaderboard/LeaderboardPage.jsx";
import { AdminLayout } from "./pages/Admin/AdminLayout.jsx";
import { AdminDashboard } from "./pages/Admin/AdminDashboard.jsx";
import { AdminUsers } from "./pages/Admin/AdminUsers.jsx";
import { AdminTopics } from "./pages/Admin/AdminTopics.jsx";
import { AdminSubjects } from "./pages/Admin/AdminSubjects.jsx";
import { AdminSubSubjects } from "./pages/Admin/AdminSubSubjects.jsx";
import { AdminMLGroups } from "./pages/Admin/AdminMLGroups.jsx";
// Lazy-loaded so the charts (recharts) + animations (framer-motion) only download
// when a student actually opens the dashboard, keeping the main bundle lean.
const StudentDashboard = lazy(() =>
  import("./pages/Dashboard/StudentDashboard.jsx").then((m) => ({ default: m.StudentDashboard }))
);

// Waits for session restore before deciding — prevents redirect on refresh
const ProtectedRoute = ({ element }) => {
  const { user, authLoading } = useContext(AuthContext);
  if (authLoading) return null;
  if (!user) return <Navigate to="/welcome" replace />;
  return element;
};

// Auth routes (login/register) — wait so a refreshed logged-in user isn't flashed the login page
const AuthRoute = ({ element }) => {
  const { user, authLoading } = useContext(AuthContext);
  if (authLoading) return null;
  if (user) return <Navigate to={user.role === "ADMIN" ? "/admin/dashboard" : "/"} replace />;
  return element;
};

// Admin-only route — waits for session restore, then requires role ADMIN
const AdminRoute = ({ element }) => {
  const { user, authLoading } = useContext(AuthContext);
  if (authLoading) return null;
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== "ADMIN") return <Navigate to="/" replace />;
  return element;
};

function AppRoutes() {
  const { user, authLoading } = useContext(AuthContext);

  return (
    <BrowserRouter>
      {!authLoading && user && <Navbar/>}
      <Routes>
        {/* Landing / intro hero — guests start here, click → login */}
        <Route path="/welcome" element={<AuthRoute element={<Landing />} />} />

        {/* Auth pages - only accessible when not logged in */}
        <Route path="/login" element={<AuthRoute element={<LoginForm />} />} />
        <Route path="/register" element={<AuthRoute element={<RegisterForm />} />} />
        <Route path="/forgot-password" element={<AuthRoute element={<ForgotPassword />} />} />

        {/* Level survey — protected but outside DashboardLayout to avoid the survey gate loop */}
        <Route path="/level-survey" element={<ProtectedRoute element={<LevelManagerPage />} />} />

        {/* Admin area — guarded layout (language switcher + dir) with nested pages */}
        <Route path="/admin" element={<AdminRoute element={<AdminLayout />} />}>
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<AdminDashboard />} />
          <Route path="users" element={<AdminUsers />} />
          <Route path="topics" element={<AdminTopics />} />
          <Route path="topics/:topicId/subjects" element={<AdminSubjects />} />
          <Route path="subjects/:subjectId/sub-subjects" element={<AdminSubSubjects />} />
          <Route path="ml-groups" element={<AdminMLGroups />} />
        </Route>

        {/* Dashboard - only accessible when logged in */}
        <Route path="/" element={<ProtectedRoute element={<DashboardLayout />} />}>
          <Route index element={<Navigate to="/home" replace />} />
          <Route path="home" element={<Home />} />
          <Route
            path="dashboard"
            element={
              <Suspense fallback={<div style={{ padding: 24, color: "#888" }}>Loading dashboard…</div>}>
                <StudentDashboard />
              </Suspense>
            }
          />
          <Route path="math-training" element={<MathTraining />} />
          <Route path="math-training/:subSubjectId/play" element={<QuestionGame />} />
          <Route path="leaderboard" element={<LeaderboardPage />} />
          <Route path="profile-settings" element={<ProfileSettings />} />
        </Route>

        {/* Default redirect */}
        <Route path="*" element={<Navigate to={!authLoading && user ? "/" : "/welcome"} replace />} />
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
