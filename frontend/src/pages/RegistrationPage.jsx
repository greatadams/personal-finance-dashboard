import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginPage.css';
import './Header.css';
export function RegistrationPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [address, setAddress] = useState('');
  const [gender, setGender] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    //VALIDATE FIRST
    if (password != confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    setIsLoading(true);

    try {
      //connect to backend
      const regResponse = await fetch(
        'http://localhost:9090/api/auth/register',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            email: email,
            password,
            confirmPassword,
            firstName,
            lastName,
            phoneNumber,
            address,
            gender,
          }),
        },
      );
      if (!regResponse.ok) {
        const errorData = await regResponse.json();
        throw new Error(errorData.message || 'Registration failed');
      }

      console.log('Registration successful! Auto-logging in...');

      // Wait 1 second for backend to finish processing
      await new Promise((resolve) => setTimeout(resolve, 1000));

      //Auto-login
      const loginResponse = await fetch(
        'http://localhost:9090/api/auth/login',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ email, password }),
        },
      );
      if (!loginResponse.ok) {
        throw new Error(
          'Registration successful but login failed.Please login manually.',
        );
      }
      const loginData = await loginResponse.json();

      //  Store tokens
      localStorage.setItem('token', loginData.accessToken);
      localStorage.setItem('userId', loginData.userId);
      localStorage.setItem('customerId', loginData.customerId);
      localStorage.setItem('email', loginData.email);
      localStorage.setItem('role', loginData.role);

      console.log('Auto-login successful!', loginData);

      // Redirect
      navigate('/accounts');
    } catch (err) {
      setError(err.message || 'An error occurred.Please try again.');
      console.log('Login error:', err);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="form">
      <div className="logo-auth">💼 FinanceApp</div>
      <h2>Registration Form</h2>

      <form className="form-container" onSubmit={handleSubmit}>
        <div className="form-input">
          <label>Email</label>
          <input
            type="email"
            name="email"
            onChange={(e) => setEmail(e.target.value)}
            disabled={isLoading}
            required
          />

          <label>Password</label>
          <input
            type="password"
            name="password"
            onChange={(e) => setPassword(e.target.value)}
            disabled={isLoading}
            required
          />

          <label>Confirm Password</label>
          <input
            type="password"
            name="password"
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={isLoading}
            required
          />

          <label>First Name</label>
          <input
            type="text"
            name="firstName"
            onChange={(e) => setFirstName(e.target.value)}
            disabled={isLoading}
            required
          />

          <label>Last Name</label>
          <input
            type="text"
            name="lastName"
            onChange={(e) => setLastName(e.target.value)}
            disabled={isLoading}
            required
          />

          <label>Phone Number</label>
          <input
            type="tel"
            name="phone-number"
            onChange={(e) => setPhoneNumber(e.target.value)}
            disabled={isLoading}
            required
          />

          <label>Address</label>
          <input
            type="text"
            name="Address"
            onChange={(e) => setAddress(e.target.value)}
            disabled={isLoading}
            required
          />

          <label>Gender</label>
          <select
            type="text"
            name="Gender"
            onChange={(e) => setGender(e.target.value)}
            disabled={isLoading}
            required
          >
            <option value="">Select Gender</option>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </select>

          <button type="submit" className="form-primary-btn">
            Submit
          </button>
          {error && <div className="error-message">{error}</div>}
        </div>
      </form>
    </div>
  );
}
