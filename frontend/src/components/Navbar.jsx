import { DEMO_STUDENTS } from '../constants';

export default function Navbar({
  currentStudent,
  onStudentChange,
  activeTab,
  onTabChange,
  onCreateClick,
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
            🗂️ All Listings & History
          </button>
        </nav>

        {/* Right Section: Student Switcher & Create Action */}
        <div className="header-actions">
          {/* Active Student Switcher */}
          <div className="student-switcher" title="Switch student context to test seller ownership & authorization">
            <span className="switcher-label">Active Student:</span>
            <div className="switcher-box">
              <span className="student-avatar">{currentStudent.avatar || '🎓'}</span>
              <select
                value={currentStudent.id}
                onChange={(e) => {
                  const selected = DEMO_STUDENTS.find(s => s.id === e.target.value);
                  if (selected) {
                    onStudentChange(selected);
                  }
                }}
                className="student-select"
              >
                {DEMO_STUDENTS.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.id})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Create Listing Button */}
          <button className="btn-primary create-btn" onClick={onCreateClick}>
            <span className="btn-icon">+</span>
            <span>List an Item</span>
          </button>
        </div>
      </div>
    </header>
  );
}
