import axios from 'axios';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import './TransactionPage.css';
import './Header.css';

function TransactionPage() {
  const [transactionList, setTransactionList] = useState([]);
  //   const [newTransaction, setNewTransaction] = useState();
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [searchParams] = useSearchParams();
  const [successMessage, setSuccessMessage] = useState('');
  const [formData, setFormData] = useState({
    fromAccountId: '',
    toAccountId: '',
    amount: '',
    description: '',
    transactionType: 'TRANSFER',
  });
  const [accounts, setAccounts] = useState([]);
  const [isExternal, setIsExternal] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const navigate = useNavigate();

  //Read a parameter
  const accountId = searchParams.get('accountId');
  //if URL is /transactions?accountId=5 -> accountId ='5'
  //if URL is /transactions -> accountId =null

  const token = localStorage.getItem('token');
  const customerId = localStorage.getItem('customerId');

  async function fetchUserAccounts() {
    try {
      const response = await axios.get(
        `http://localhost:9090/api/accounts/customer/${customerId}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        },
      );

      setAccounts(response.data);
    } catch (err) {
      setError(err.message || 'An error occurred');
    }
  }

  async function handleCreateTransaction(e) {
    e.preventDefault();
    const idempotencyKey = crypto.randomUUID();
    const requestBody = {
      customerId: customerId,
      idempotencyKey: idempotencyKey,
      amount: formData.amount,
      fromAccountId: formData.fromAccountId,
      toAccountId: formData.toAccountId,
      description: formData.description,
      transactionType: formData.transactionType,
    };

    try {
      const response = await axios.post(
        `http://localhost:9090/api/transactions`,
        requestBody,
        {
          headers: {
            'Content-Type': 'Application/json',
            Authorization: `Bearer ${token}`,
          },
        },
      );
      setSuccessMessage('Transaction created successfully!');
      setTimeout(() => setSuccessMessage(''), 3000);
      handleTransactions();
      setShowForm(false);
      setFormData({
        fromAccountId: '',
        toAccountId: '',
        amount: '',
        description: '',
        transactionType: 'TRANSFER',
      });
    } catch (err) {
      setError(err.message || 'An error occurred');
    }
  }

  async function handleTransactions() {
    try {
      const response = await axios.get(
        `http://localhost:9090/api/transactions/customer/${customerId}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        },
      );
      setTransactionList(response.data);
      setIsLoading(false);
    } catch (err) {
      setError(err.message || 'An error occurred');
      console.log('Failed to load transactions:', err);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    handleTransactions();
    fetchUserAccounts();

    // Pre-fill FROM account if coming from a specific account
    if (accountId) {
      setFormData((prev) => ({ ...prev, fromAccountId: accountId }));
    }
  }, []);

  const filteredTransactions = accountId
    ? transactionList.filter(
        (tx) =>
          tx.fromAccountId === parseInt(accountId) ||
          tx.toAccountId === parseInt(accountId),
      )
    : transactionList;

  const selectedAccount = accounts.find(
    (acct) => acct.id === parseInt(accountId),
  );

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div className="error">{error}</div>;

  function handleLogout() {
    localStorage.clear();
    navigate('/login');
  }
  return (
    <div className="transaction-page">
      <div className="account-header">
        {/* Left: Logo */}
        <div className="logo" onClick={() => navigate('/accounts')}>
          💼 FinanceApp
        </div>

        {/* Right: Navigation buttons */}
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
      {selectedAccount && (
        <div className="account-dashboard">
          <h3>{selectedAccount.accountName}</h3>
          <p>Account #:{selectedAccount.accountNumber}</p>
          <p>Account Type:{selectedAccount.accountType}</p>
          <p className=" account-balance">
            Account Balance:${selectedAccount.accountBalance}
          </p>
        </div>
      )}
      {!showForm ? (
        <button onClick={() => setShowForm(true)}>Create Transaction</button>
      ) : (
        <div className="transaction-form">
          <h2>Create Transaction</h2>
          <form onSubmit={handleCreateTransaction}>
            <label>From Account:</label>
            <select
              value={formData.fromAccountId}
              onChange={(e) =>
                setFormData({ ...formData, fromAccountId: e.target.value })
              }
              required
            >
              <option value="">Select Account</option>
              {accounts.map((acct) => (
                <option key={acct.id} value={acct.id}>
                  {acct.accountName} - {acct.accountNumber}
                </option>
              ))}
            </select>
            <label>
              <input
                type="checkbox"
                checked={isExternal}
                onChange={(e) => setIsExternal(e.target.checked)}
              />
              Transfer to external account
            </label>

            {isExternal ? (
              <input
                type="text"
                placeholder="Enter account number"
                value={formData.toAccountId}
                onChange={(e) =>
                  setFormData({ ...formData, toAccountId: e.target.value })
                }
                required
              />
            ) : (
              <select
                value={formData.toAccountId}
                onChange={(e) =>
                  setFormData({ ...formData, toAccountId: e.target.value })
                }
                required
              >
                <option value="">Select Account</option>
                {accounts.map((acct) => (
                  <option key={acct.id} value={acct.id}>
                    {acct.accountName} - {acct.accountNumber}
                  </option>
                ))}
              </select>
            )}
            <label>Amount:</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              placeholder="0.00"
              value={formData.amount}
              onChange={(e) =>
                setFormData({ ...formData, amount: e.target.value })
              }
              required
            />
            <label>Transaction Type:</label>
            <select
              value={formData.transactionType}
              onChange={(e) =>
                setFormData({ ...formData, transactionType: e.target.value })
              }
            >
              <option value="TRANSFER">Transfer</option>
              <option value="DEPOSIT">Deposit</option>
              <option value="WITHDRAWAL">Withdrawal</option>
              <option value="PAYMENT">Payment</option>
            </select>
            <label>Description (optional):</label>
            <input
              type="text"
              placeholder="What is this transaction for?"
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
            />

            <button type="submit">Submit</button>
            {successMessage && (
              <div className="success-message">{successMessage}</div>
            )}
            {error && <div className="error-message">{error}</div>}
          </form>
        </div>
      )}
      {successMessage && (
        <div className="success-message">{successMessage}</div>
      )}

      <div>
        <h2>Transaction History</h2>
        {filteredTransactions.length === 0 ? (
          <p>No transactions found for this account.</p>
        ) : (
          filteredTransactions.map((tx) => (
            <div key={tx.id} className="transaction-card">
              <div className="transaction-header">
                <span className="transaction-amount">${tx.amount}</span>
                <span
                  className={`transaction-status ${tx.transactionStatus.toLowerCase()}`}
                >
                  {tx.transactionStatus}
                </span>
              </div>
              <div className="transaction-flow">
                <span>From Account: {tx.fromAccountId}</span>
                <span>→</span>
                <span>To Account: {tx.toAccountId}</span>
              </div>
              <div className="transaction-details">
                <span className="transaction-type">{tx.transactionType}</span>
                <span className="transaction-date">
                  {new Date(tx.transactionDate).toLocaleDateString()}
                </span>
              </div>
              {tx.description && (
                <div className="transaction-description">{tx.description}</div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default TransactionPage;
