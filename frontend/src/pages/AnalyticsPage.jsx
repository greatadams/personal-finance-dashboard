import { useState, useEffect } from 'react';
import './Header.css';
import axios from 'axios';
import './AnalyticsPage.css';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { useNavigate } from 'react-router-dom';

export function AnalyticsPage() {
  const [analyticsData, setAnalyticsData] = useState([]);
  const [year, setYear] = useState(new Date().getFullYear());
  const [isLoading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const token = localStorage.getItem('token');
  const customerId = localStorage.getItem('customerId');

  useEffect(() => {
    fetchAnalytics();
  }, [year]);

  async function fetchAnalytics() {
    setLoading(true);

    try {
      const response = await axios.get(
        `http://localhost:9090/api/analytics/customer/${customerId}/year/${year}`,
        {
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
        },
      );
      setAnalyticsData(response.data);
    } catch (err) {
      setError(err.message || 'Analytics not found');
    } finally {
      setLoading(false);
    }
  }

  if (isLoading) return <div>Loading Analytics...</div>;

  if (error) return <div className="error">{error}</div>;

  // Calculate totals
  const totalSpent = analyticsData.reduce(
    (sum, item) => sum + parseFloat(item.totalSpent),
    0,
  );
  const totalReceived = analyticsData.reduce(
    (sum, item) => sum + parseFloat(item.totalReceived),
    0,
  );
  const totalTransactions = analyticsData.reduce(
    (sum, item) => sum + item.transactionCount,
    0,
  );
  // Prepare chart data - all 12 months
  const chartData = [
    'JANUARY',
    'FEBRUARY',
    'MARCH',
    'APRIL',
    'MAY',
    'JUNE',
    'JULY',
    'AUGUST',
    'SEPTEMBER',
    'OCTOBER',
    'NOVEMBER',
    'DECEMBER',
  ].map((month) => {
    const monthData = analyticsData.find((item) => item.month === month);
    return {
      month: month.slice(0, 3), // "JAN", "FEB", etc
      spent: monthData ? parseFloat(monthData.totalSpent) : 0,
      received: monthData ? parseFloat(monthData.totalReceived) : 0,
    };
  });

  function handleLogout() {
    localStorage.clear();
    navigate('/login');
  }
  // Generate last 3 years dynamically
  const currentYear = new Date().getFullYear();
  const years = [currentYear - 2, currentYear - 1, currentYear];
  return (
    <div className="analytics-dashboard">
      <div className="account-header">
        <div className="logo" onClick={() => navigate('/accounts')}>
          💼 FinanceApp
        </div>
        <div className="header-buttons">
          <div className="avatar-circle" onClick={() => navigate('/customer')}>
            👤
          </div>
          <button
            className="analytics-btn"
            onClick={() => navigate('/analytics')}
          >
            📈 Analytics
          </button>
          <button className="logout-btn" onClick={handleLogout}>
            ⏻ Logout
          </button>
        </div>
      </div>
      <h1 className="analytics-header">Analytics Dashboard</h1>

      <select
        className="analytics-year"
        value={year}
        onChange={(e) => setYear(Number(e.target.value))}
      >
        {years.map((yr) => (
          <option key={yr} value={yr}>
            {yr}
          </option>
        ))}
      </select>
      <div className="summary-cards">
        <div className="summary-card">
          <h3>Total Spent</h3>
          <p className="amount spent">${totalSpent.toFixed(2)}</p>
        </div>

        <div className="summary-card">
          <h3>Total Received</h3>
          <p className="amount received">${totalReceived.toFixed(2)}</p>
        </div>

        <div className="summary-card">
          <h3>Transactions</h3>
          <p className="count">{totalTransactions}</p>
        </div>
      </div>

      <div className="chart-container">
        <h2>Monthly Overview</h2>

        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="month" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="spent" fill="#EF4444" name="Spent" />
            <Bar dataKey="received" fill="#10B981" name="Received" />
          </BarChart>
        </ResponsiveContainer>
      </div>
      <p className="analytics-month">
        Loaded: {analyticsData.length} month of data
      </p>
    </div>
  );
}
