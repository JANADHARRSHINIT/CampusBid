import { useState, useEffect, useCallback } from 'react';
import { wantedApi } from '../api/wantedApi';
import { useAuth } from '../context/AuthContext';
import WantedItemModal from './WantedItemModal';

export default function WantedDashboard({
  onViewListing,
  showToast,
  onWantedCountChange,
}) {
  const { user, isAuthenticated, openAuthModal } = useAuth();

  const [wantedItems, setWantedItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  const loadItems = useCallback(async () => {
    if (!isAuthenticated) {
      setWantedItems([]);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError('');
      const data = await wantedApi.getMyWantedItems();
      const items = Array.isArray(data) ? data : [];
      setWantedItems(items);
      if (onWantedCountChange) {
        onWantedCountChange(items.length);
      }
    } catch (err) {
      setError(err.message || 'Unable to load your wanted items. Please verify your connection or try again.');
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated, onWantedCountChange]);

  useEffect(() => {
    loadItems();
  }, [loadItems]);

  const handleToggleStatus = async (item) => {
    const newStatus = item.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
    try {
      const updated = await wantedApi.toggleStatus(item.id, newStatus);
      showToast(
        newStatus === 'ACTIVE'
          ? `Alert for "${item.itemName}" resumed.`
          : `Alert for "${item.itemName}" paused.`,
        'info'
      );
      setWantedItems((prev) =>
        prev.map((it) => (it.id === item.id ? { ...it, status: updated.status } : it))
      );
      // Refresh to ensure all backend counts and matches are perfectly synced
      loadItems();
    } catch (err) {
      showToast(err.message || 'Failed to update alert status.', 'error');
    }
  };

  const handleDelete = async (item) => {
    if (!window.confirm(`Are you sure you want to delete your alert for "${item.itemName}"?`)) {
      return;
    }

    setDeletingId(item.id);
    try {
      await wantedApi.deleteWantedItem(item.id);
      showToast(`Wanted alert for "${item.itemName}" removed.`, 'info');
      setWantedItems((prev) => prev.filter((it) => it.id !== item.id));
      // Refresh to ensure all backend counts and matches are perfectly synced
      loadItems();
    } catch (err) {
      showToast(err.message || 'Failed to delete alert.', 'error');
    } finally {
      setDeletingId(null);
    }
  };

  if (!isAuthenticated) {
    return (
      <section className="wanted-dashboard-section">
        <div className="wanted-auth-required-card">
          <div className="auth-card-icon">🎯</div>
          <h2 className="auth-card-title">Wanted Items & Smart Alerts</h2>
          <p className="auth-card-desc">
            Never miss an item you need. Sign in to your verified campus account to post requests, monitor incoming listings in real time, and receive instant alerts when a classmate lists matching gear.
          </p>
          <button
            type="button"
            className="btn-primary btn-lg"
            onClick={() => openAuthModal('login', 'Please sign in to access Wanted Items & Smart Alerts.')}
          >
            Sign In to Create Alerts
          </button>
        </div>
      </section>
    );
  }

  const activeCount = wantedItems.filter((i) => i.status === 'ACTIVE').length;
  const totalMatches = wantedItems.reduce((acc, curr) => acc + (curr.matchCount || (curr.matches ? curr.matches.length : 0)), 0);

  return (
    <section className="wanted-dashboard-section" aria-label="Student Wanted Items Dashboard">
      {/* Hero Header */}
      <div className="wanted-header-card">
        <div className="wanted-header-info">
          <span className="wanted-pill-tag">🎯 Smart Campus Radar</span>
          <h1 className="wanted-header-title">Wanted Items & Smart Alerts</h1>
          <p className="wanted-header-desc">
            Post what you're looking for. CampusBid monitors every new auction listing 24/7 and alerts you in-app and by email the second a classmate lists a matching lot within your budget.
          </p>
        </div>

        <div className="wanted-header-actions">
          <button
            type="button"
            className="btn-primary btn-lg btn-post-wanted"
            onClick={() => {
              setEditingItem(null);
              setIsModalOpen(true);
            }}
          >
            <span className="btn-icon">+</span>
            <span>Post What You Need</span>
          </button>
        </div>
      </div>

      {/* Stats Summary Bar - Displays real counts from backend data, or dash while error */}
      <div className="wanted-stats-bar">
        <div className="wanted-stat-card">
          <span className="stat-label">Total Requests</span>
          <span className="stat-value">{error ? '—' : wantedItems.length}</span>
        </div>
        <div className="wanted-stat-card">
          <span className="stat-label">Active Monitoring</span>
          <span className="stat-value text-green">{error ? '—' : activeCount}</span>
        </div>
        <div className="wanted-stat-card">
          <span className="stat-label">Matching Lots Found</span>
          <span className="stat-value text-orange">{error ? '—' : totalMatches}</span>
        </div>
      </div>

      {/* Error state with retry option */}
      {error && (
        <div className="bid-alert-banner alert-error" style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span className="alert-icon">⚠️</span>
            <span>{error}</span>
          </div>
          <button
            type="button"
            className="btn-secondary btn-sm"
            onClick={loadItems}
            disabled={loading}
            style={{ whiteSpace: 'nowrap' }}
          >
            🔄 Try Again
          </button>
        </div>
      )}

      {/* Content Grid / Empty state / Error state */}
      {loading ? (
        <div className="loading-state">
          <div className="spinner-large"></div>
          <p>Scanning your wanted alerts and matched listings...</p>
        </div>
      ) : error ? (
        <div className="wanted-empty-card" style={{ borderColor: 'var(--bidding-orange, #e65100)' }}>
          <div className="empty-icon-wrap" style={{ background: '#fff7ed', color: '#ea580c' }}>⚠️</div>
          <h3 className="empty-title">Unable to display wanted items</h3>
          <p className="empty-desc">
            {error}
          </p>
          <button
            type="button"
            className="btn-primary"
            onClick={loadItems}
          >
            🔄 Retry Loading
          </button>
        </div>
      ) : wantedItems.length === 0 ? (
        <div className="wanted-empty-card">
          <div className="empty-icon-wrap">🎯</div>
          <h3 className="empty-title">No wanted items posted yet</h3>
          <p className="empty-desc">
            Searching for a bicycle, engineering textbook, dorm chair, or graphing calculator? Post your request with your maximum budget, and CampusBid will automatically alert you when a classmate lists it.
          </p>
          <button
            type="button"
            className="btn-primary btn-post-first"
            onClick={() => {
              setEditingItem(null);
              setIsModalOpen(true);
            }}
          >
            + Post What You Need (Free)
          </button>
        </div>
      ) : (
        <div className="wanted-cards-grid">
          {wantedItems.map((item) => {
            const matches = item.matches || [];
            const hasMatches = matches.length > 0;

            return (
              <article
                key={item.id}
                className={`wanted-item-card ${item.status === 'PAUSED' ? 'card-paused' : ''}`}
              >
                {/* Card Top Row: Title, Category, Status */}
                <div className="wanted-card-top">
                  <div>
                    <div className="wanted-title-row">
                      <h3 className="wanted-card-title">{item.itemName}</h3>
                      <span className={`wanted-status-badge ${item.status === 'ACTIVE' ? 'badge-active' : 'badge-paused'}`}>
                        {item.status === 'ACTIVE' ? '● Active Alert' : '⏸ Paused'}
                      </span>
                    </div>
                    {item.category && (
                      <span className="wanted-category-chip">🏷️ {item.category}</span>
                    )}
                  </div>

                  <div className="wanted-budget-box">
                    <span className="budget-label">Max Budget</span>
                    <span className="budget-amount">₹{Number(item.maxBudget).toLocaleString('en-IN')}</span>
                  </div>
                </div>

                {/* Card Specs */}
                <div className="wanted-specs-row">
                  <div className="spec-item">
                    <span className="spec-title">Preferred Condition:</span>
                    <span className="spec-val">
                      {item.preferredCondition === 'ANY' ? 'Any Condition' : item.preferredCondition.replace('_', ' ')}
                    </span>
                  </div>
                  <div className="spec-item">
                    <span className="spec-title">Created:</span>
                    <span className="spec-val">
                      {new Date(item.createdAt).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })}
                    </span>
                  </div>
                </div>

                {item.additionalRequirements && (
                  <p className="wanted-notes-text">
                    <strong>Notes:</strong> {item.additionalRequirements}
                  </p>
                )}

                {/* MATCHED LOTS SECTION */}
                <div className="wanted-matches-hub">
                  <div className="matches-hub-header">
                    <span className="matches-hub-title">
                      {hasMatches
                        ? `🎉 Found ${matches.length} Matching ${matches.length === 1 ? 'Auction' : 'Auctions'}`
                        : item.status === 'ACTIVE'
                        ? '🔍 Radar Active: Checking every incoming auction lot'
                        : '⏸ Alert paused'}
                    </span>
                  </div>

                  {hasMatches ? (
                    <div className="matched-lots-scroll">
                      {matches.map((m) => (
                        <div key={m.id} className="matched-lot-item">
                          <img
                            src={m.listingPhoto || 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=200&q=80'}
                            alt={m.listingTitle}
                            className="matched-lot-thumb"
                          />
                          <div className="matched-lot-info">
                            <span className="matched-lot-title">{m.listingTitle}</span>
                            <span className="matched-lot-price">
                              Starts at <strong>₹{Number(m.listingPrice).toLocaleString('en-IN')}</strong>
                            </span>
                          </div>
                          <button
                            type="button"
                            className="btn-view-match"
                            onClick={() => onViewListing(m.listingId)}
                            title="Open auction details and place bid"
                          >
                            View Lot →
                          </button>
                        </div>
                      ))}
                    </div>
                  ) : item.status === 'ACTIVE' ? (
                    <div className="matching-radar-pulse">
                      <span className="radar-dot"></span>
                      <span>No matching items listed yet. You will get alerted the instant one is posted under ₹{Number(item.maxBudget).toLocaleString('en-IN')}.</span>
                    </div>
                  ) : (
                    <div className="matching-paused-text">
                      Alert monitoring is paused for this request. Click "Resume Alert" to reactivate.
                    </div>
                  )}
                </div>

                {/* Card Controls */}
                <div className="wanted-card-actions">
                  <button
                    type="button"
                    className={`btn-action-pill ${item.status === 'ACTIVE' ? 'btn-pause' : 'btn-resume'}`}
                    onClick={() => handleToggleStatus(item)}
                    title={item.status === 'ACTIVE' ? 'Pause automatic alerts' : 'Resume automatic alerts'}
                  >
                    {item.status === 'ACTIVE' ? '⏸ Pause Alert' : '▶ Resume Alert'}
                  </button>

                  <button
                    type="button"
                    className="btn-action-pill btn-edit"
                    onClick={() => {
                      setEditingItem(item);
                      setIsModalOpen(true);
                    }}
                    title="Edit request details and budget"
                  >
                    ✏️ Edit
                  </button>

                  <button
                    type="button"
                    className="btn-action-pill btn-delete"
                    onClick={() => handleDelete(item)}
                    disabled={deletingId === item.id}
                    title="Delete this wanted alert"
                  >
                    {deletingId === item.id ? 'Deleting...' : '🗑️ Delete'}
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {/* Create / Edit Modal */}
      <WantedItemModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setEditingItem(null);
        }}
        initialItem={editingItem}
        onSuccess={() => {
          loadItems();
        }}
        showToast={showToast}
      />
    </section>
  );
}
