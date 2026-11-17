import { Navigate, Route, Routes } from "react-router-dom";

import { Header } from "./components/Header";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { MyPage } from "./pages/MyPage";
import { SignupPage } from "./pages/SignupPage";

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
        <Route path="/" element={<Navigate to="/mypage" replace />} />
      </Routes>
    </>
  );
}

export default App;
