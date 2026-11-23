import { Navigate, Route, Routes } from "react-router-dom";

import { Header } from "./components/Header";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { MyPage } from "./pages/MyPage";
import { SignupPage } from "./pages/SignupPage";
import { StudyDetailPage } from "./pages/StudyDetailPage";
import { StudyListPage } from "./pages/StudyListPage";
import { StudyPostFormPage } from "./pages/StudyPostFormPage";

function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/mypage"
          element={
            <ProtectedRoute>
              <MyPage />
            </ProtectedRoute>
          }
        />
        <Route path="/studies" element={<StudyListPage />} />
        <Route
          path="/studies/new"
          element={
            <ProtectedRoute>
              <StudyPostFormPage />
            </ProtectedRoute>
          }
        />
        <Route path="/studies/:postId" element={<StudyDetailPage />} />
        <Route
          path="/studies/:postId/edit"
          element={
            <ProtectedRoute>
              <StudyPostFormPage />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/studies" replace />} />
      </Routes>
    </>
  );
}

export default App;
