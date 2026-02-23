import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Header.css';
import './CustomerPage.css';
export function CustomerPage() {
  const [customerDetails, setCustomerDetails] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    address: '',
  });

  const navigate = useNavigate();
  const token = localStorage.getItem('token');
  const customerId = localStorage.getItem('customerId');

  async function displayCustomerDetails() {
    setError('');
    setLoading(true);

    try {
      //connect to backend
      const cxResponse = await fetch(
        `http://localhost:9090/api/customers/${customerId}`,
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
        },
      );
      //check if successful
      if (!cxResponse.ok) {
        const errorData = await cxResponse.json();
        throw new Error(
          errorData.message || 'Failed to fetch customer details',
        );
      }

      //parse the json
      const data = await cxResponse.json();

      //store in state
      setCustomerDetails(data);
      setEditForm(data);

      console.log('Customer details loaded', data);
    } catch (err) {
      setError(err.message || 'An error occurred.Please try again.');
      console.log('Login error:', err);
    } finally {
      setLoading(false);
    }
  }
  function handleLogout() {
    localStorage.clear();
    navigate('/login');
  }

  useEffect(() => {
    if (!token) {
      navigate('/accounts');
      return;
    }

    displayCustomerDetails();
  }, [navigate]);

  async function handleEditCustomer() {
    try {
      const response = await fetch(
        `http://localhost:9090/api/customers/${customerId}`,
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(editForm),
        },
      );
      //check if successful
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Update failed');
      }

      const data = await response.json();
      setCustomerDetails(data); // update displayed data
      setIsEditing(false); // close edit mode
    } catch (err) {
      setError(err.message || 'An error occurred');
      console.log('Create account error:', err);
    } finally {
      setLoading(false);
    }
  }
  if (loading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div className="error-banner">{error}</div>;
  }

  return (
    <div className="project-page">
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
      {/* header */}
      <div className="profile-header">
        <h1>Your Profile</h1>
        <div className="profile-header-actions">
          <button className="edit-btn" onClick={() => setIsEditing(true)}>
            Edit Profile
          </button>
        </div>
      </div>

      {/* Avatar + name */}

      <div className="profile-card">
        <div className="profile-avatar">
          {customerDetails.firstName?.charAt(0).toUpperCase()}
          {customerDetails.lastName?.charAt(0).toUpperCase()}
        </div>
        <h2 className="profile-name">
          {customerDetails.firstName} {customerDetails.lastName}
        </h2>
        <p className="profile-email">
          <h3>{customerDetails.email}</h3>
        </p>

        {/* Details */}
        <div className="profile-details">
          {isEditing ? (
            //show input field
            <div className="edit-form">
              <input
                value={editForm.firstName}
                onChange={(e) =>
                  setEditForm({ ...editForm, firstName: e.target.value })
                }
              />
              <input
                value={editForm.lastName}
                onChange={(e) =>
                  setEditForm({ ...editForm, lastName: e.target.value })
                }
              />
              <input
                value={editForm.email}
                onChange={(e) =>
                  setEditForm({ ...editForm, email: e.target.value })
                }
              />
              <input
                value={editForm.phoneNumber}
                onChange={(e) =>
                  setEditForm({ ...editForm, phoneNumber: e.target.value })
                }
              />
              <input
                value={editForm.address}
                onChange={(e) =>
                  setEditForm({ ...editForm, address: e.target.value })
                }
              />
              <button className="save-btn" onClick={handleEditCustomer}>
                Save
              </button>
              <button
                className="cancel-btn"
                onClick={() => setIsEditing(false)}
              >
                Cancel
              </button>
            </div>
          ) : (
            <div>
              <h3>Account Details</h3>
              <div className="detail-row">
                <span className="detail-label">Phone</span>
                <span className="detail-value">
                  {customerDetails.phoneNumber}
                </span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Email</span>
                <span className="detail-value">{customerDetails.email}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Address</span>
                <span className="detail-value">{customerDetails.address}</span>
              </div>

              <div className="detail-row">
                <span className="detail-label">Member Since</span>
                <span className="detail-value">
                  {new Date(customerDetails.createdAt).toLocaleDateString()}
                </span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
