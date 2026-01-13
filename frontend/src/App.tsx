import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import MatchListPage from './pages/MatchListPage';
import SeatSelectionPage from './pages/SeatSelectionPage';
import ReservationConfirmPage from './pages/ReservationConfirmPage';
import Header from './components/Header';

function App() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="App">
      {isAuthenticated && <Header />}
      <Routes>
        <Route path="/login" element={!isAuthenticated ? <LoginPage /> : <Navigate to="/" />} />
        <Route
          path="/"
          element={isAuthenticated ? <MatchListPage /> : <Navigate to="/login" />}
        />
        <Route
          path="/match/:matchId"
          element={isAuthenticated ? <SeatSelectionPage /> : <Navigate to="/login" />}
        />
        <Route
          path="/reservation/confirm"
          element={isAuthenticated ? <ReservationConfirmPage /> : <Navigate to="/login" />}
        />
      </Routes>
    </div>
  );
}

export default App;
