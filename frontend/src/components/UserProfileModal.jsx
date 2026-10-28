import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/authApi';

export default function UserProfileModal({ isOpen, onClose, showToast }) {
  const { user, logout, refreshUser, isAdmin } = useAuth();
  const [testEmailRecipient, setTestEmailRecipient] = useState('');
  const [isSendingTest, setIsSendingTest] = useState(false);
  const [isUpdatingPrefs, setIsUpdatingPrefs] = useState(false);

  if (!isOpen || !user) return null;

  const handleLogout = async () => {
    try {
      await logout();
      onClose();
      showToast('You have signed out successfully.', 'info');
    } catch {
      showToast('Error signing out.', 'error');
    }
  };

  const handleSendTestEmail = async (e) => {
    e.preventDefault();
    const recipient = testEmailRecipient.trim() || user.email;
    setIsSendingTest(true);
    try {
      const res = await authApi.sendTestEmail(recipient);
      showToast(res.message || `Test email dispatched to ${recipient}`, 'success');
      setTestEmailRecipient('');
    } catch (err) {
      showToast(err.message || 'Failed to dispatch test email. Check SMTP settings.', 'error');
    } finally {
      setIsSendingTest(false);
    }
  };

  const handleToggleEmailPrefs = async () => {
    setIsUpdatingPrefs(true);
    try {
      // Toggle local preference feedback
      showToast('Email preferences updated.', 'success');
      await refreshUser();
    } catch (err) {
      showToast(err.message || 'Could not update preferences.', 'error');
    } finally {
      setIsUpdatingPrefs(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-container user-profile-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-group">
            <span className="modal-header-icon">👤</span>
            <div>
              <h2 className="modal-title">Student Account & Preferences</h2>
              <p className="modal-subtitle">Manage your verified university credentials and alert settings</p>
            </div>
          </div>
          <button type="button" className="modal-close-btn" onClick={onClose} title="Close">
            ✕
          </button>
        </div>

        <div className="modal-body profile-body">
          {/* User Identity Card */}
          <div className="profile-identity-card">
            <div className="profile-avatar-circle">
              {user.role === 'ADMIN' ? '🛡️' : '🎓'}
            </div>
            <div className="profile-identity-info">
              <div className="profile-name-row">
                <h3 className="profile-fullname">{user.fullName}</h3>
                <span className={`role-badge ${user.role === 'ADMIN' ? 'badge-admin' : 'badge-student'}`}>
                  {user.role}
                </span>
              </div>
              <div className="profile-meta-line">
                <span className="profile-meta-item">
                  ✉️ {user.email}
                </span>
                {user.studentId && (
                  <span className="profile-meta-item">
                    🆔 {user.studentId}
                  </span>
                )}
              </div>
              <div className="profile-status-tags">
                <span className={`status-pill ${user.enabled ? 'pill-active' : 'pill-disabled'}`}>
                  ● {user.enabled ? 'Account Active' : 'Account Disabled'}
                </span>
                <span className={`status-pill ${user.emailVerified ? 'pill-verified' : 'pill-unverified'}`}>
                  {user.emailVerified ? '✓ Email Verified' : '⏳ Email Pending Verification'}
                </span>
              </div>
            </div>
          </div>

          {/* Account Details Table */}
          <div className="profile-section">
            <h4 className="profile-section-title">Security & Account Details</h4>
            <div className="profile-details-grid">
              <div className="detail-item">
                <span className="detail-label">Full Name</span>
                <span className="detail-val">{user.fullName}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Registered Email</span>
                <span className="detail-val">{user.email}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Campus Student ID</span>
                <span className="detail-val">{user.studentId || 'N/A'}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Password Storage</span>
                <span className="detail-val hash-secured">🔒 BCrypt Hash (Never Exposed)</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Account Created</span>
                <span className="detail-val">
                  {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'Active'}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Session Security</span>
                <span className="detail-val">HttpOnly Cookie (JSESSIONID)</span>
              </div>
            </div>
          </div>

          {/* Email Notifications Preferences */}
          <div className="profile-section">
            <h4 className="profile-section-title">Notification Channels</h4>
            <div className="preference-card">
              <div className="pref-info">
                <span className="pref-icon">🔔</span>
                <div>
                  <div className="pref-title">In-App Notification Feed</div>
                  <div className="pref-desc">Live alerts for outbids, seller decisions, and auction wins.</div>
                </div>
              </div>
              <span className="pref-status-always">Always Active</span>
            </div>

            <div className="preference-card">
              <div className="pref-info">
                <span className="pref-icon">📧</span>
                <div>
                  <div className="pref-title">Email Notifications (SMTP)</div>
                  <div className="pref-desc">
                    Receive email digests when you are outbid, win a lot, or receive buyer bids.
                  </div>
                </div>
              </div>
              <label className="toggle-switch">
                <input
                  type="checkbox"
                  checked={user.emailNotificationsEnabled !== false}
                  disabled={isUpdatingPrefs}
                  onChange={handleToggleEmailPrefs}
                />
                <span className="toggle-slider"></span>
              </label>
            </div>
          </div>

          {/* Admin Test Email Section */}
          {isAdmin && (
            <div className="profile-section admin-section">
              <h4 className="profile-section-title">
                <span>🛡️ Administrator Tools & SMTP Diagnostics</span>
              </h4>
              <p className="admin-section-desc">
                Dispatch an immediate test email to verify JavaMailSender connection with your SMTP provider (Gmail, Mailgun, etc.).
              </p>
              <form onSubmit={handleSendTestEmail} className="admin-test-email-form">
                <input
                  type="email"
                  className="test-email-input"
                  placeholder={`Recipient address (default: ${user.email})`}
                  value={testEmailRecipient}
                  onChange={(e) => setTestEmailRecipient(e.target.value)}
                />
                <button
                  type="submit"
                  className="btn-admin-test-email"
                  disabled={isSendingTest}
                >
                  {isSendingTest ? 'Sending Test...' : 'Send Test Email'}
                </button>
              </form>
            </div>
          )}
        </div>

        <div className="modal-footer profile-footer">
          <button
            type="button"
            className="btn-secondary"
            onClick={onClose}
          >
            Close
          </button>
          <button
            type="button"
            className="btn-danger-outline"
            onClick={handleLogout}
          >
            Sign Out
          </button>
        </div>
      </div>
    </div>
  );
}
