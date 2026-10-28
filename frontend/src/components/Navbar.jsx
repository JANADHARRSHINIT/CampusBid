import { CAMPUS_STUDENTS } from '../constants';

export default function Navbar({
  currentStudent,
  onStudentChange,
  activeTab,
  onTabChange,
  onCreateClick,
  onNotificationsClick,
  unreadNotifCount = 0,
  stats,
}) {
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

        {/* Tab Navigation */}
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
            className={`nav-tab ${activeTab === 'all' ? 'active' : ''}`}
            onClick={() => onTabChange('all')}
          >
            🗂️ All Auctions
          </button>
        </nav>

        {/* Right Section: Student Switcher, Notifications & Create Action */}
        <div className="header-actions">
          {/* Notifications Button */}
          <button
            type="button"
            className="btn-notifications-nav"
            onClick={onNotificationsClick}
            title="View your auction and bidding notifications"
          >
            <span>🔔</span>
            <span className="notif-nav-label">Notifications</span>
            {unreadNotifCount > 0 && (
              <span className="nav-notif-badge">{unreadNotifCount}</span>
            )}
          </button>

          {/* Active Student Switcher */}
          <div className="student-switcher" title="Active Student Profile">
            <span className="switcher-label">Account:</span>
            <div className="switcher-box">
              <span className="student-avatar">{currentStudent.avatar || '🎓'}</span>
              <select
                value={currentStudent.id}
                onChange={(e) => {
                  const selected = CAMPUS_STUDENTS.find(s => s.id === e.target.value);
                  if (selected) {
                    onStudentChange(selected);
                  }
                }}
                className="student-select"
              >
                {CAMPUS_STUDENTS.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Create Listing Button */}
          <button className="btn-primary create-btn" onClick={onCreateClick}>
            <span className="btn-icon">+</span>
            <span>Create Listing</span>
          </button>
        </div>
      </div>
    </header>
  );
}
