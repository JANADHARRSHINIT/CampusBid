import { useEffect, useState } from 'react';
import { notificationApi } from '../api/notificationApi';
import { formatFullDate } from '../constants';

const NOTIF_ICONS = {
  OUTBID: '⚠️',
  BID_ACCEPTED: '🔥',
  AUCTION_WON: '🏆',
  AWAITING_APPROVAL: '⚡',
  BID_APPROVED: '✅',
  BID_REJECTED: '❌',
  AUCTION_CLOSED: '🔒',
  BID_RECEIVED: '📥',
  HANDOVER_CONFIRMED: '🤝',
  SALE_COMPLETED: '🎉',
  SALE_CANCELLED: '🛑',
  WANTED_ITEM_MATCH: '🎯',
};

export default function NotificationModal({
  isOpen,
  onClose,
  currentStudent,
  onViewListing,
  onNotificationsUpdated,
}) {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);

  useEffect(() => {
    if (!isOpen || !currentStudent?.id) return;
    loadNotifications();
  }, [isOpen, currentStudent?.id]);

  async function loadNotifications() {
    try {
      setLoading(true);
      const data = await notificationApi.getNotifications(currentStudent.id);
      setNotifications(data || []);
    } catch {
      // Graceful fallback
    } finally {
      setLoading(false);
    }
  }

  async function handleMarkAsRead(notif) {
    if (notif.read) return;
    try {
      await notificationApi.markAsRead(notif.id, currentStudent.id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notif.id ? { ...n, read: true } : n))
      );
      if (onNotificationsUpdated) onNotificationsUpdated();
    } catch {
      // ignore
    }
  }

  async function handleMarkAllAsRead() {
    try {
      setMarkingAll(true);
      await notificationApi.markAllAsRead(currentStudent.id);
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      if (onNotificationsUpdated) onNotificationsUpdated();
    } catch {
      // ignore
    } finally {
      setMarkingAll(false);
    }
  }

  if (!isOpen) return null;

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-container modal-md" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="notif-modal-title-row">
            <h2 className="modal-title">🔔 My Notifications</h2>
            {unreadCount > 0 && (
              <span className="notif-unread-count-pill">{unreadCount} New</span>
            )}
          </div>
          <div className="notif-header-actions">
            {unreadCount > 0 && (
              <button
                type="button"
                className="btn-mark-all-read"
                onClick={handleMarkAllAsRead}
                disabled={markingAll}
              >
                {markingAll ? 'Marking...' : 'Mark all read'}
              </button>
            )}
            <button className="modal-close-btn" onClick={onClose}>✕</button>
          </div>
        </div>

        <div className="notif-modal-body">
          {loading ? (
            <div className="notif-loading-state">
              <div className="spinner"></div>
              <span>Loading notifications...</span>
            </div>
          ) : notifications.length === 0 ? (
            <div className="notif-empty-state">
              <span className="notif-empty-icon">📭</span>
              <h4>No notifications yet</h4>
              <p>You'll receive updates here when you place bids, win auctions, or receive bids on your listings.</p>
            </div>
          ) : (
            <div className="notif-list">
              {notifications.map((n) => {
                const icon = NOTIF_ICONS[n.type] || '🔔';

                return (
                  <div
                    key={n.id}
                    className={`notif-item ${!n.read ? 'notif-unread' : ''}`}
                    onClick={() => handleMarkAsRead(n)}
                  >
                    <div className="notif-icon-col">
                      <span className="notif-type-icon">{icon}</span>
                      {!n.read && <span className="notif-dot" title="Unread notification"></span>}
                    </div>

                    <div className="notif-content-col">
                      <div className="notif-title-row">
                        <h4 className="notif-item-title">{n.title}</h4>
                        <span className="notif-time">{formatFullDate(n.createdAt)}</span>
                      </div>
                      <p className="notif-message">{n.message}</p>

                      {n.relatedListingId && (
                        <div className="notif-listing-link-row">
                          <button
                            type="button"
                            className="btn-notif-view-lot"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleMarkAsRead(n);
                              if (onViewListing) onViewListing(n.relatedListingId);
                              onClose();
                            }}
                          >
                            👁️ View Lot #{n.relatedListingId} &rarr;
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button type="button" className="btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
