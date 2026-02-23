import { useState } from 'react';
import './LoginPage.css';
import './Header.css';
import { useNavigate } from 'react-router-dom';

export function LoginPage() {
  const [emailOrUsername, setEmailOrUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      // Send request through API Gateway
      const loginResponse = await fetch(
        'http://localhost:9090/api/auth/login',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include', //IMPORTANT:SO BROWSER STORES REFRESH COOKIES
          body: JSON.stringify({
            email: emailOrUsername,
            password,
          }),
        },
      );

      //check if response is successful
      if (!loginResponse.ok) {
        const errorData = await loginResponse.json();
        throw new Error(errorData.message || 'Login Failed');
      }

      const data = await loginResponse.json();
      // Store the access token
      localStorage.setItem('token', data.accessToken);

      // Store user info
      localStorage.setItem('userId', data.userId);
      localStorage.setItem('customerId', data.customerId);
      localStorage.setItem('email', data.email);
      localStorage.setItem('role', data.role);

      console.log('login successful!', data);

      //redirect to account page(dashboard)
      navigate('/accounts');
    } catch (err) {
      setError(err.message || 'An error occurred.Please try again.');
      console.log('Login error:', err);
    } finally {
      setIsLoading(false);
    }
  }

  //   //REFRESH CALL(when an access token expires)
  //   const refreshToken = async () => {
  //     const refreshResponse = await fetch(
  //       'http://localhost:8086/api/auth/refresh',
  //       {
  //         method: 'POST',
  //         credentials: 'include',
  //       },
  //     );

  //     const data = await refreshResponse.json();
  //     localStorage.setItem('token', data.accessToken);
  //   };

  return (
    <div className="form">
      <div className="logo-auth">💼 FinanceApp</div>

      <form className="form-container" onSubmit={handleSubmit}>
        <h2>Sign In</h2>

        {error && <div className="error-message">{error}</div>}

        <div className="form-input">
          <input
            type="text"
            name="email"
            id="email"
            placeholder="Email or Username"
            value={emailOrUsername}
            onChange={(e) => setEmailOrUsername(e.target.value)}
            required
            disabled={isLoading}
          />

          <input
            type="password"
            id="password"
            name="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            disabled={isLoading}
          />

          <button
            type="submit"
            className="form-primary-btn"
            disabled={isLoading}
          >
            {isLoading ? 'Logging in...' : 'Login'}
          </button>
        </div>

        <div className="form-secondary-btn">
          <button type="button" disabled={isLoading}>
            Forgot Password
          </button>
          <button
            type="button"
            className="secondary-btn"
            onClick={() => navigate('/register')}
            disabled={isLoading}
          >
            Register
          </button>
        </div>
      </form>
    </div>
  );
}
