import { useState, useEffect, useId } from 'react';
import { useAuth } from '../context/AuthContext';

export default function AuthModal({ showToast }) {
  const { authModalState, closeAuthModal, login, register } = useAuth();
  const { isOpen, initialTab, promptMessage } = authModalState;

  const [activeTab, setActiveTab] = useState(initialTab || 'login');
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Password visibility toggles
  const [showLoginPassword, setShowLoginPassword] = useState(false);
  const [showRegPassword, setShowRegPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Login Form State
  const [loginForm, setLoginForm] = useState({
    email: 'alex.chen@campus.edu',
    password: 'Password123!',
  });
  const [loginErrors, setLoginErrors] = useState({});

  // Register Form State
  const [registerForm, setRegisterForm] = useState({
    fullName: '',
    email: '',
    studentId: '',
    password: '',
    confirmPassword: '',
    agreeTerms: true,
  });
  const [registerErrors, setRegisterErrors] = useState({});

  // Unique IDs for accessibility
  const loginEmailId = useId();
  const loginPassId = useId();
  const regNameId = useId();
  const regEmailId = useId();
  const regStudentId = useId();
  const regPassId = useId();
  const regConfirmPassId = useId();

  useEffect(() => {
    if (initialTab) {
      setActiveTab(initialTab);
    }
    setErrorMessage('');
    setSuccessMessage('');
    setLoginErrors({});
    setRegisterErrors({});
  }, [initialTab, isOpen]);

  if (!isOpen) return null;

  // Password strength calculation
  const calculatePasswordStrength = (pass) => {
    if (!pass) return { score: 0, label: 'None', color: '#cbd5e1' };
    let score = 0;
    if (pass.length >= 8) score += 1;
    if (/[A-Z]/.test(pass) && /[a-z]/.test(pass)) score += 1;
    if (/[0-9]/.test(pass)) score += 1;
    if (/[^A-Za-z0-9]/.test(pass)) score += 1;

    switch (score) {
      case 1:
        return { score: 1, label: 'Weak', color: '#ef4444' };
      case 2:
        return { score: 2, label: 'Fair', color: '#f59e0b' };
      case 3:
        return { score: 3, label: 'Good', color: '#10b981' };
      case 4:
        return { score: 4, label: 'Strong', color: '#059669' };
      default:
        return { score: 0, label: 'Very Weak', color: '#ef4444' };
    }
  };

  const passStrength = calculatePasswordStrength(registerForm.password);

  const handleQuickFill = (email, password) => {
    setActiveTab('login');
    setLoginForm({ email, password });
    setErrorMessage('');
    setLoginErrors({});
  };

  const validateLogin = () => {
    const errors = {};
    if (!loginForm.email.trim()) {
      errors.email = 'Email address is required.';
    } else if (!loginForm.email.includes('@')) {
      errors.email = 'Please enter a valid email address.';
    }
    if (!loginForm.password) {
      errors.password = 'Password is required.';
    }
    setLoginErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!validateLogin()) {
      return;
    }

    setSubmitting(true);
    try {
      const user = await login(loginForm.email.trim(), loginForm.password);
      if (showToast) {
        showToast(`Welcome back, ${user.fullName}!`, 'success');
      }
    } catch (err) {
      setErrorMessage(err.message || 'Invalid email or password. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const validateRegister = () => {
    const errors = {};
    if (!registerForm.fullName.trim() || registerForm.fullName.trim().length < 2) {
      errors.fullName = 'Full name must be at least 2 characters.';
    }
    if (!registerForm.email.trim() || !registerForm.email.includes('@')) {
      errors.email = 'A valid college email address is required.';
    }
    if (!registerForm.password || registerForm.password.length < 8) {
      errors.password = 'Password must be at least 8 characters long.';
    }
    if (registerForm.password !== registerForm.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
    }
    if (!registerForm.agreeTerms) {
      errors.agreeTerms = 'You must acknowledge the campus honor code.';
    }
    setRegisterErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!validateRegister()) {
      return;
    }

    setSubmitting(true);
    try {
      const user = await register({
        fullName: registerForm.fullName.trim(),
        email: registerForm.email.trim(),
        password: registerForm.password,
        studentId: registerForm.studentId.trim() || undefined,
      });

      if (showToast) {
        showToast(`Account verified! Welcome to CampusBid, ${user.fullName}.`, 'success');
      }
    } catch (err) {
      setErrorMessage(err.message || 'Registration failed. An account with this email may already exist.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-overlay-backdrop" onClick={closeAuthModal} role="dialog" aria-modal="true">
      <div className="auth-split-card" onClick={(e) => e.stopPropagation()}>
        {/* Close Button */}
        <button
          type="button"
          className="auth-close-circle-btn"
          onClick={closeAuthModal}
          aria-label="Close authentication dialog"
        >
          ✕
        </button>

        {/* LEFT COLUMN: Marketplace Brand Showcase */}
        <div className="auth-brand-showcase">
          <div className="showcase-decor-pattern" aria-hidden="true"></div>
          <div className="showcase-content">
            <div className="showcase-logo-badge">
              <span className="showcase-logo-icon">🏷️</span>
              <span className="showcase-logo-text">Campus<span className="accent">Bid</span></span>
            </div>

            <div className="showcase-headline-group">
              <span className="showcase-tag">🎪 University Flea Market & Auction</span>
              <h2 className="showcase-title">Trade, Bid & Connect With Your Campus</h2>
              <p className="showcase-subtitle">
                The student marketplace built exclusively for verified university peers. Buy pre-loved gear, bid in real-time, and arrange safe physical campus handovers.
              </p>
            </div>

            {/* Feature Highlights Grid */}
            <div className="showcase-features-list">
              <div className="showcase-feature-item">
                <div className="feature-icon-bubble">🛡️</div>
                <div>
                  <h4 className="feature-title">Verified Student Network</h4>
                  <p className="feature-desc">All sellers and bidders are university-verified students.</p>
                </div>
              </div>

              <div className="showcase-feature-item">
                <div className="feature-icon-bubble">🔨</div>
                <div>
                  <h4 className="feature-title">Live Fair Auctions</h4>
                  <p className="feature-desc">Bid on textbooks, bikes, and dorm tech with automatic closing.</p>
                </div>
              </div>

              <div className="showcase-feature-item">
                <div className="feature-icon-bubble">🎯</div>
                <div>
                  <h4 className="feature-title">Wanted Items Smart Alerts</h4>
                  <p className="feature-desc">Post what you need and get alerted when a classmate lists it.</p>
                </div>
              </div>
            </div>

            {/* Trust Footer */}
            <div className="showcase-trust-bar">
              <span>🔒 BCrypt Secured</span>
              <span>•</span>
              <span>🤝 In-Person Campus Pickup</span>
              <span>•</span>
              <span>🎓 Zero Hidden Fees</span>
            </div>
          </div>
        </div>

        {/* RIGHT COLUMN: Interactive Sign In / Register Form */}
        <div className="auth-form-container">
          {/* Header & Tabs */}
          <div className="auth-form-header">
            <div className="auth-tab-pills" role="tablist">
              <button
                type="button"
                role="tab"
                aria-selected={activeTab === 'login'}
                className={`auth-pill-tab ${activeTab === 'login' ? 'active' : ''}`}
                onClick={() => {
                  setActiveTab('login');
                  setErrorMessage('');
                  setSuccessMessage('');
                }}
              >
                Sign In
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={activeTab === 'register'}
                className={`auth-pill-tab ${activeTab === 'register' ? 'active' : ''}`}
                onClick={() => {
                  setActiveTab('register');
                  setErrorMessage('');
                  setSuccessMessage('');
                }}
              >
                Create Account
              </button>
            </div>

            <h3 className="auth-action-heading">
              {activeTab === 'login' ? 'Welcome Back, Student' : 'Join Your Campus Marketplace'}
            </h3>
            <p className="auth-action-sub">
              {activeTab === 'login'
                ? 'Sign in to place bids, manage lots, and view your alerts'
                : 'Create your free verified account with your university email'}
            </p>
          </div>

          {/* Action Prompt Banner (e.g. from clicking Place Bid while logged out) */}
          {promptMessage && (
            <div className="auth-prompt-callout" role="alert">
              <span className="callout-icon">⚡</span>
              <span className="callout-text">{promptMessage}</span>
            </div>
          )}

          {/* Error Message Alert */}
          {errorMessage && (
            <div className="auth-alert-box alert-error" role="alert">
              <span className="alert-icon">⚠️</span>
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Success Message Alert */}
          {successMessage && (
            <div className="auth-alert-box alert-success" role="alert">
              <span className="alert-icon">✓</span>
              <span>{successMessage}</span>
            </div>
          )}

          {/* SIGN IN FORM */}
          {activeTab === 'login' ? (
            <form onSubmit={handleLoginSubmit} className="auth-interactive-form" noValidate>
              <div className="auth-field-group">
                <label htmlFor={loginEmailId} className="auth-field-label">
                  Campus Email Address <span className="req">*</span>
                </label>
                <div className="auth-input-wrapper">
                  <span className="input-affix-icon">✉️</span>
                  <input
                    id={loginEmailId}
                    type="email"
                    autoComplete="email"
                    className={`auth-text-input ${loginErrors.email ? 'input-invalid' : ''}`}
                    placeholder="student@campus.edu"
                    value={loginForm.email}
                    onChange={(e) => {
                      setLoginForm({ ...loginForm, email: e.target.value });
                      if (loginErrors.email) setLoginErrors({ ...loginErrors, email: '' });
                    }}
                    required
                  />
                </div>
                {loginErrors.email && (
                  <span className="auth-inline-error">{loginErrors.email}</span>
                )}
              </div>

              <div className="auth-field-group">
                <div className="label-with-hint">
                  <label htmlFor={loginPassId} className="auth-field-label">
                    Password <span className="req">*</span>
                  </label>
                </div>
                <div className="auth-input-wrapper">
                  <span className="input-affix-icon">🔒</span>
                  <input
                    id={loginPassId}
                    type={showLoginPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    className={`auth-text-input ${loginErrors.password ? 'input-invalid' : ''}`}
                    placeholder="Enter your password"
                    value={loginForm.password}
                    onChange={(e) => {
                      setLoginForm({ ...loginForm, password: e.target.value });
                      if (loginErrors.password) setLoginErrors({ ...loginErrors, password: '' });
                    }}
                    required
                  />
                  <button
                    type="button"
                    className="auth-password-toggle-btn"
                    onClick={() => setShowLoginPassword(!showLoginPassword)}
                    title={showLoginPassword ? 'Hide password' : 'Show password'}
                    aria-label={showLoginPassword ? 'Hide password' : 'Show password'}
                  >
                    {showLoginPassword ? '👁️‍🗨️' : '👁️'}
                  </button>
                </div>
                {loginErrors.password && (
                  <span className="auth-inline-error">{loginErrors.password}</span>
                )}
              </div>

              <button
                type="submit"
                className="btn-primary auth-submit-btn"
                disabled={submitting}
              >
                {submitting ? (
                  <span className="btn-loading-content">
                    <span className="auth-spinner"></span> Signing In...
                  </span>
                ) : (
                  <span>Sign In to CampusBid →</span>
                )}
              </button>

              <div className="auth-switch-prompt">
                <span>New to CampusBid?</span>{' '}
                <button
                  type="button"
                  className="auth-link-btn"
                  onClick={() => {
                    setActiveTab('register');
                    setErrorMessage('');
                  }}
                >
                  Create student account
                </button>
              </div>

              {/* 1-Click Demo Fillers for Testing & Evaluation */}
              <div className="auth-demo-fillers">
                <span className="demo-fillers-label">⚡ 1-Click Demo Profiles (For Testing):</span>
                <div className="demo-pill-grid">
                  <button
                    type="button"
                    className="demo-pill-btn"
                    onClick={() => handleQuickFill('alex.chen@campus.edu', 'Password123!')}
                  >
                    🎓 Alex Chen (Seller)
                  </button>
                  <button
                    type="button"
                    className="demo-pill-btn"
                    onClick={() => handleQuickFill('maya.patel@campus.edu', 'Password123!')}
                  >
                    🎓 Maya Patel (Buyer)
                  </button>
                  <button
                    type="button"
                    className="demo-pill-btn"
                    onClick={() => handleQuickFill('rohan.sharma@campus.edu', 'Password123!')}
                  >
                    🎓 Rohan Sharma
                  </button>
                  <button
                    type="button"
                    className="demo-pill-btn demo-pill-admin"
                    onClick={() => handleQuickFill('admin@campusbid.edu', 'AdminPassword123!')}
                  >
                    🛡️ Administrator
                  </button>
                </div>
              </div>
            </form>
          ) : (
            /* REGISTER FORM */
            <form onSubmit={handleRegisterSubmit} className="auth-interactive-form" noValidate>
              <div className="auth-field-group">
                <label htmlFor={regNameId} className="auth-field-label">
                  Full Name <span className="req">*</span>
                </label>
                <div className="auth-input-wrapper">
                  <span className="input-affix-icon">👤</span>
                  <input
                    id={regNameId}
                    type="text"
                    autoComplete="name"
                    className={`auth-text-input ${registerErrors.fullName ? 'input-invalid' : ''}`}
                    placeholder="e.g. Priyanshu Verma"
                    value={registerForm.fullName}
                    onChange={(e) => {
                      setRegisterForm({ ...registerForm, fullName: e.target.value });
                      if (registerErrors.fullName) setRegisterErrors({ ...registerErrors, fullName: '' });
                    }}
                    required
                  />
                </div>
                {registerErrors.fullName && (
                  <span className="auth-inline-error">{registerErrors.fullName}</span>
                )}
              </div>

              <div className="auth-row-dual">
                <div className="auth-field-group">
                  <label htmlFor={regEmailId} className="auth-field-label">
                    College Email <span className="req">*</span>
                  </label>
                  <div className="auth-input-wrapper">
                    <span className="input-affix-icon">✉️</span>
                    <input
                      id={regEmailId}
                      type="email"
                      autoComplete="email"
                      className={`auth-text-input ${registerErrors.email ? 'input-invalid' : ''}`}
                      placeholder="name@college.edu"
                      value={registerForm.email}
                      onChange={(e) => {
                        setRegisterForm({ ...registerForm, email: e.target.value });
                        if (registerErrors.email) setRegisterErrors({ ...registerErrors, email: '' });
                      }}
                      required
                    />
                  </div>
                  {registerErrors.email && (
                    <span className="auth-inline-error">{registerErrors.email}</span>
                  )}
                </div>

                <div className="auth-field-group">
                  <label htmlFor={regStudentId} className="auth-field-label">
                    Student ID / Roll No.
                  </label>
                  <div className="auth-input-wrapper">
                    <span className="input-affix-icon">🆔</span>
                    <input
                      id={regStudentId}
                      type="text"
                      className="auth-text-input"
                      placeholder="e.g. 2024-CS-042"
                      value={registerForm.studentId}
                      onChange={(e) => setRegisterForm({ ...registerForm, studentId: e.target.value })}
                    />
                  </div>
                </div>
              </div>

              <div className="auth-field-group">
                <label htmlFor={regPassId} className="auth-field-label">
                  Create Password <span className="req">*</span>
                </label>
                <div className="auth-input-wrapper">
                  <span className="input-affix-icon">🔑</span>
                  <input
                    id={regPassId}
                    type={showRegPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    className={`auth-text-input ${registerErrors.password ? 'input-invalid' : ''}`}
                    placeholder="Minimum 8 characters"
                    value={registerForm.password}
                    onChange={(e) => {
                      setRegisterForm({ ...registerForm, password: e.target.value });
                      if (registerErrors.password) setRegisterErrors({ ...registerErrors, password: '' });
                    }}
                    required
                  />
                  <button
                    type="button"
                    className="auth-password-toggle-btn"
                    onClick={() => setShowRegPassword(!showRegPassword)}
                    title={showRegPassword ? 'Hide password' : 'Show password'}
                    aria-label={showRegPassword ? 'Hide password' : 'Show password'}
                  >
                    {showRegPassword ? '👁️‍🗨️' : '👁️'}
                  </button>
                </div>
                {registerErrors.password && (
                  <span className="auth-inline-error">{registerErrors.password}</span>
                )}

                {/* Password Strength Indicator */}
                {registerForm.password && (
                  <div className="auth-pass-strength-panel">
                    <div className="strength-meter-bar">
                      <div
                        className="strength-meter-fill"
                        style={{
                          width: `${(passStrength.score / 4) * 100}%`,
                          backgroundColor: passStrength.color,
                        }}
                      ></div>
                    </div>
                    <div className="strength-label-row">
                      <span className="strength-text" style={{ color: passStrength.color }}>
                        Password Strength: <strong>{passStrength.label}</strong>
                      </span>
                      <span className="strength-tip">
                        {passStrength.score < 3 ? 'Mix letters, numbers & symbols' : '✓ Strong password'}
                      </span>
                    </div>
                  </div>
                )}
              </div>

              <div className="auth-field-group">
                <label htmlFor={regConfirmPassId} className="auth-field-label">
                  Confirm Password <span className="req">*</span>
                </label>
                <div className="auth-input-wrapper">
                  <span className="input-affix-icon">🔒</span>
                  <input
                    id={regConfirmPassId}
                    type={showConfirmPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    className={`auth-text-input ${registerErrors.confirmPassword ? 'input-invalid' : ''}`}
                    placeholder="Re-enter password"
                    value={registerForm.confirmPassword}
                    onChange={(e) => {
                      setRegisterForm({ ...registerForm, confirmPassword: e.target.value });
                      if (registerErrors.confirmPassword) setRegisterErrors({ ...registerErrors, confirmPassword: '' });
                    }}
                    required
                  />
                  <button
                    type="button"
                    className="auth-password-toggle-btn"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    title={showConfirmPassword ? 'Hide password' : 'Show password'}
                    aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                  >
                    {showConfirmPassword ? '👁️‍🗨️' : '👁️'}
                  </button>
                </div>
                {registerErrors.confirmPassword && (
                  <span className="auth-inline-error">{registerErrors.confirmPassword}</span>
                )}
              </div>

              {/* Honor Code & Agreement */}
              <label className="auth-checkbox-label">
                <input
                  type="checkbox"
                  checked={registerForm.agreeTerms}
                  onChange={(e) => {
                    setRegisterForm({ ...registerForm, agreeTerms: e.target.checked });
                    if (registerErrors.agreeTerms) setRegisterErrors({ ...registerErrors, agreeTerms: '' });
                  }}
                />
                <span className="checkbox-text">
                  I agree to the <strong>CampusBid Honor Code</strong> & commit to fair bidding and safe on-campus handover.
                </span>
              </label>
              {registerErrors.agreeTerms && (
                <span className="auth-inline-error">{registerErrors.agreeTerms}</span>
              )}

              <button
                type="submit"
                className="btn-primary auth-submit-btn"
                disabled={submitting}
              >
                {submitting ? (
                  <span className="btn-loading-content">
                    <span className="auth-spinner"></span> Creating Account...
                  </span>
                ) : (
                  <span>Create Student Account →</span>
                )}
              </button>

              <div className="auth-switch-prompt">
                <span>Already have a verified account?</span>{' '}
                <button
                  type="button"
                  className="auth-link-btn"
                  onClick={() => {
                    setActiveTab('login');
                    setErrorMessage('');
                  }}
                >
                  Sign in here
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
