/**
 * CampusBid Notifications API Client
 */

function getHeaders(studentId, isJson = true) {
  const headers = {};
  if (isJson) {
    headers['Content-Type'] = 'application/json';
  }
  if (studentId) {
    headers['X-Student-Id'] = studentId;
  }
  return headers;
}

export const notificationApi = {
  /**
   * Fetch all notifications for the active student.
   */
  async getNotifications(studentId) {
    const res = await fetch('/api/notifications', {
      headers: getHeaders(studentId, false),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch notifications');
    }
    return data.data || [];
  },

  /**
   * Fetch unread notification count.
   */
  async getUnreadCount(studentId) {
    const res = await fetch('/api/notifications/unread-count', {
      headers: getHeaders(studentId, false),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch unread count');
    }
    return data.data || 0;
  },

  /**
   * Mark a single notification as read.
   */
  async markAsRead(id, studentId) {
    const res = await fetch(`/api/notifications/${id}/read`, {
      method: 'POST',
      headers: getHeaders(studentId, true),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to mark notification as read');
    }
    return data.data;
  },

  /**
   * Mark all notifications as read.
   */
  async markAllAsRead(studentId) {
    const res = await fetch('/api/notifications/read-all', {
      method: 'POST',
      headers: getHeaders(studentId, true),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to mark all as read');
    }
    return data.data;
  },
};
