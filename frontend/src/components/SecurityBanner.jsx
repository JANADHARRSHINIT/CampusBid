import { useState } from 'react';

export default function SecurityBanner({ currentStudent }) {
  const [dismissed, setDismissed] = useState(false);

  if (dismissed) return null;

  return (
    <div className="security-banner">
      <div className="security-banner-content">
        <div className="security-icon">🛡️</div>
        <div className="security-text">
          <strong>Student Ownership & Security Notice:</strong>{' '}
          Full authentication (JWT / SSO) is not yet integrated. Your active profile is transmitted as{' '}
          <code>X-Student-Id: {currentStudent.id}</code> ({currentStudent.name}). The Spring Boot backend strictly verifies that only the seller who created an auction can edit or cancel it. Try switching students above to verify that you cannot modify another student's listings!
        </div>
      </div>
      <button className="banner-close-btn" onClick={() => setDismissed(true)} title="Dismiss notice">
        ✕
      </button>
    </div>
  );
}
