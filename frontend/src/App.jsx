import './App.css';
import { Routes, Route } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { AccountPage } from './pages/AccountPage';
import { RegistrationPage } from './pages/RegistrationPage';
import { CustomerPage } from './pages/CustomerPage';
import TransactionPage from './pages/TransactionPage';
import { AnalyticsPage } from './pages/AnalyticsPage';

function App() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/accounts" element={<AccountPage />}></Route>
      <Route path="/register" element={<RegistrationPage />}></Route>
      <Route path="/customer" element={<CustomerPage />}></Route>
      <Route path="/transactions" element={<TransactionPage />}></Route>
      <Route path="/analytics" element={<AnalyticsPage />}></Route>
    </Routes>
  );
}

export default App;
