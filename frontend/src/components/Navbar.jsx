import { useAuth } from '../context/AuthContext';

export default function Navbar({
  activeTab,
  onTabChange,
  onCreateClick,
  onNotificationsClick,
  onProfileClick,
  unreadNotifCount = 0,
  stats,
}) {
  const { user, isAuthenticated, isAdmin, openAuthModal, logout } = useAuth();

  const handleCreate = () => {
    if (!isAuthenticated) {
      openAuthModal('login', 'Please sign in to create an auction listing.');
      return;
    }
    onCreateClick();
  };

  const handleNotifications = () => {
    if (!isAuthenticated) {
      openAuthModal('login', 'Please sign in to view your auction notifications.');
      return;
    }
    onNotificationsClick();
  };

  return (
    <header className="site-header">
      <div className="header-inner">
        {/* Logo */}
        <div className="brand" onClick={() => onTabChange('active')} style={{ cursor: 'pointer' }}>
          <div className="brand-icon">
            <span>🏷️</span>
          </div>
          <div className="brand-text">
            <span className="brand-title">Campus<span className="accent-text">Bid</span></span>
            <span className="brand-tag">Student Marketplace & Auction</span>
          </div>
        </div>

        {/* Tab Navigation (Authenticated Users Only) */}
        {isAuthenticated && (
          <nav className="header-nav">
            <button
              className={`nav-tab ${activeTab === 'active' ? 'active' : ''}`}
              onClick={() => onTabChange('active')}
            >
              🔥 Active Auctions
              {stats?.activeCount !== undefined && (
                <span className="count-pill">{stats.activeCount}</span>
              )}
            </button>
            <button
              className={`nav-tab ${activeTab === 'decisions' ? 'active' : ''} ${stats?.pendingCount > 0 ? 'nav-tab-urgent' : ''}`}
              onClick={() => onTabChange('decisions')}
              title="Closed lots awaiting seller decision"
            >
              ⚡ Decisions
              {stats?.pendingCount !== undefined && stats.pendingCount > 0 && (
                <span className="count-pill pill-alert">{stats.pendingCount}</span>
              )}
            </button>
            <button
              className={`nav-tab ${activeTab === 'won' ? 'active' : ''}`}
              onClick={() => onTabChange('won')}
              title="Lots won by you"
            >
              🏆 Won Lots
              {stats?.wonCount !== undefined && stats.wonCount > 0 && (
                <span className="count-pill pill-success">{stats.wonCount}</span>
              )}
            </button>
            <button
              className={`nav-tab ${activeTab === 'my-listings' ? 'active' : ''}`}
              onClick={() => onTabChange('my-listings')}
            >
              📦 My Listings
              {stats?.myCount !== undefined && (
                <span className="count-pill">{stats.myCount}</span>
              )}
            </button>
            <button
              className={`nav-tab ${activeTab === 'wanted' ? 'active' : ''}`}
              onClick={() => onTabChange('wanted')}
              title="Post what you need and get smart match alerts"
            >
              🎯 Wanted Alerts
              {stats?.wantedCount !== undefined && stats.wantedCount > 0 && (
                <span className="count-pill pill-alert">{stats.wantedCount}</span>
              )}
            </button>
            <button
              className={`nav-tab ${activeTab === 'all' ? 'active' : ''}`}
              onClick={() => onTabChange('all')}
            >
              🗂️ All Auctions
            </button>
          </nav>
        )}

        {/* Right Section: Auth State, Notifications & Create Action */}
        <div className="header-actions">
          {isAuthenticated ? (
            <>
              {/* Notifications Button */}
              <button
                type="button"
                className="btn-notifications-nav"
                onClick={handleNotifications}
                title="View your auction and bidding notifications"
              >
                <span>🔔</span>
                <span className="notif-nav-label">Notifications</span>
                {unreadNotifCount > 0 && (
                  <span className="nav-notif-badge">{unreadNotifCount}</span>
                )}
              </button>

              {/* User Profile & Logout */}
              <div className="user-nav-dropdown">
                <button
                  type="button"
                  className="btn-user-profile"
                  onClick={onProfileClick}
                  title="View your verified campus profile and preferences"
                >
                  <span className="student-avatar">{isAdmin ? '🛡️' : '🎓'}</span>
                  <span className="user-profile-name">{user?.fullName?.split(' ')[0] || 'Account'}</span>
                  {isAdmin && <span className="nav-role-badge">ADMIN</span>}
                </button>
                <button
                  type="button"
                  className="btn-logout-nav"
                  onClick={logout}
                  title="Sign out of CampusBid"
                >
                  Logout
                </button>
              </div>

              {/* Create Listing Button */}
              <button className="btn-primary create-btn" onClick={handleCreate}>
                <span className="btn-icon">+</span>
                <span>Create Listing</span>
              </button>
            </>
          ) : (
            <div className="auth-btn-group">
              <button
                type="button"
                className="btn-signin-nav"
                onClick={() => openAuthModal('login')}
              >
                Sign In
              </button>
              <button
                type="button"
                className="btn-register-nav"
                onClick={() => openAuthModal('register')}
              >
                Register
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
