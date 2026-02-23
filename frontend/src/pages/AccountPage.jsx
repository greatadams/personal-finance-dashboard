import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './AccountPage.css';
import './Header.css';

export function AccountPage() {
  const [accountName, setAccountName] = useState('');
  const [accountType, setAccountType] = useState('SAVINGS');
  const [accounts, setAccounts] = useState([]);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();

  const existingTypes = accounts.map((acc) => acc.accountType);

  //check if user is logged in
  useEffect(() => {
    const token = localStorage.getItem('token');

    if (!token) {
      navigate('/login');
    } else {
      fetchAccounts();
    }
  }, [navigate]);

  async function handleCreateAccount(e) {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const token = localStorage.getItem('token');
      const customerId = localStorage.getItem('customerId');

      // Make sure token exists
      if (!token) {
        setError('Please login first');
        navigate('/login');
        return;
      }

      //send request through API GATEWAY
      const response = await fetch('http://localhost:9090/api/accounts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`, //ADD JWT TOKEN
        },
        body: JSON.stringify({
          customerId: parseInt(customerId), //From localStorage
          accountName,
          accountType,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to create account');
      }

      const data = await response.json();

      //Add to account list
      setAccounts([...accounts, data]);

      //clear form
      setAccountName('');
      setAccountType('SAVINGS');
    } catch (err) {
      setError(err.message || 'An error occurred');
      console.log('Create account error:', err);
    } finally {
      setIsLoading(false);
    }
  }

  async function fetchAccounts() {
    try {
      const token = localStorage.getItem('token');
      const customerId = localStorage.getItem('customerId');

      const response = await fetch(
        `http://localhost:9090/api/accounts/customer/${customerId}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        },
      );
      if (response.ok) {
        const data = await response.json();
        setAccounts(data);
      }
    } catch (err) {
      setError(err.message || 'Account not Found');
    }
  }
  function handleLogout() {
    localStorage.clear();
    navigate('/login');
  }
  // //get email initial
  // const getEmailIn =
  //   localStorage.getItem('email')?.charAt(0).toUpperCase() || '?';

  return (
    <div className="account-page">
      <div className="account-header">
        <div className="logo" onClick={() => navigate('/accounts')}>
          💼 FinanceApp
        </div>
        <div className="header-buttons">
          <div className="avatar-circle" onClick={() => navigate('/customer')}>
            👤
          </div>
          <button
            onClick={() => navigate('/analytics')}
            className="analytics-btn"
          >
            {' '}
            📈 Analytics
          </button>
          <button className="logout-btn" onClick={handleLogout}>
            ⏻ Logout
          </button>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {accounts.length >= 3 ? (
        <div className="create-account-section">
          <p>You have all available account types!</p>
        </div>
      ) : (
        <>
          <div className="create-account-section">
            <h2>Create New Account</h2>
          </div>
          <form onSubmit={handleCreateAccount} className="account-form">
            <input
              type="text"
              placeholder="Account Name"
              value={accountName}
              onChange={(e) => setAccountName(e.target.value)}
              required
              disabled={isLoading}
            />

            <select
              value={accountType}
              onChange={(e) => setAccountType(e.target.value)}
              disabled={isLoading}
            >
              <option
                value="SAVINGS"
                disabled={existingTypes.includes('SAVINGS')}
              >
                Savings {existingTypes.includes('SAVINGS') && '✓'}
              </option>
              <option
                value="CHECKING"
                disabled={existingTypes.includes('CHECKING')}
              >
                Checking {existingTypes.includes('CHECKING') && '✓'}
              </option>
              <option
                value="CREDIT"
                disabled={existingTypes.includes('CREDIT')}
              >
                Credit {existingTypes.includes('CREDIT') && '✓'}
              </option>
            </select>

            <button type="submit" disabled={isLoading}>
              {isLoading ? 'Creating...' : 'Create Account'}
            </button>
          </form>
        </>
      )}
      <div className="accounts-list-section">
        <h2>Your Accounts</h2>
        {accounts.length === 0 ? (
          <p>No accounts yet. Create one above!</p>
        ) : (
          <ul className="accounts-list">
            {accounts.map((account) => (
              <li
                key={account.id}
                className="account-item"
                onClick={() =>
                  navigate(`/transactions?accountId=${account.id}`)
                }
                style={{ cursor: 'pointer' }}
              >
                <strong>{account.accountName}</strong>
                <span className="account-type"> {account.accountType}</span>
                <br />
                <span className="account-number">{account.accountNumber}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
