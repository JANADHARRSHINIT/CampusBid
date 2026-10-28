const BASE_URL = '/api/auth';

/**
 * Client for CampusBid Student & Admin Authentication APIs.
 * All requests include credentials (cookies) to persist the secure session.
 */
export const authApi = {
  /**
   * Register a new student account.
   */
  async register(formData) {
    const res = await fetch(`${BASE_URL}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(formData),
    });
    const data = await res.json();
    if (!res.ok) {
      const errorMsg = data.errors
        ? Object.values(data.errors).join(' ')
        : (data.message || 'Registration failed. Please check your information.');
      throw new Error(errorMsg);
    }
    return data.data;
  },

  /**
   * Log in with college email and password.
   */
  async login(credentials) {
    const res = await fetch(`${BASE_URL}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(credentials),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Invalid email or password.');
    }
    return data.data;
  },

  /**
   * Retrieve current authenticated user profile across page reloads.
   */
  async getMe() {
    try {
      const res = await fetch(`${BASE_URL}/me`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
      });
      if (!res.ok) {
        return null;
      }
      const data = await res.json();
      return data.data || null;
    } catch {
      return null;
    }
  },

  /**
   * Log out active student and destroy server session.
   */
  async logout() {
    try {
      const res = await fetch(`${BASE_URL}/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
      });
      const data = await res.json();
      return data.success;
    } catch {
      return true;
    }
  },

  /**
   * Verify email address using single-use verification token.
   */
  async verifyEmail(token) {
    const res = await fetch(`${BASE_URL}/verify-email?token=${encodeURIComponent(token)}`, {
      method: 'GET',
      credentials: 'include',
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Email verification link is invalid or expired.');
    }
    return data.data;
  },

  /**
   * Send test email notification (Admin only).
   */
  async sendTestEmail(email) {
    const res = await fetch(`/api/admin/test-email?email=${encodeURIComponent(email)}`, {
      method: 'POST',
      credentials: 'include',
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to dispatch test email.');
    }
    return data.data;
  },
};
