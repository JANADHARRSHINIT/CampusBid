import { useState, useEffect, useCallback } from 'react';
import Navbar from './components/Navbar';
import SecurityBanner from './components/SecurityBanner';
import ListingCard from './components/ListingCard';
import CreateListingModal from './components/CreateListingModal';
import EditListingModal from './components/EditListingModal';
import CancelModal from './components/CancelModal';
import ListingDetailModal from './components/ListingDetailModal';
import Toast from './components/Toast';
import { listingApi } from './api/listingApi';
import { DEMO_STUDENTS, CATEGORIES, CATEGORY_ICONS } from './constants';
import './App.css';

export default function App() {
  const [currentStudent, setCurrentStudent] = useState(DEMO_STUDENTS[0]);
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('active'); // 'active' | 'my-listings' | 'all'
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [conditionFilter, setConditionFilter] = useState('ALL');

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [detailTarget, setDetailTarget] = useState(null);

  // Toast notification
  const [toast, setToast] = useState(null);

  const showToast = useCallback((message, type = 'info') => {
    setToast({ message, type });
  }, []);

  // Fetch listings based on current tab and filters
  const loadListings = useCallback(async () => {
    try {
      let data = [];
      if (activeTab === 'my-listings') {
        data = await listingApi.getMyListings(currentStudent.id);
      } else if (activeTab === 'all') {
        data = await listingApi.getAllListings('', selectedCategory, searchKeyword);
      } else {
        // 'active'
        data = await listingApi.getActiveListings(selectedCategory, searchKeyword);
      }
      setListings(data);
    } catch (err) {
      showToast(err.message || 'Could not load listings from server.', 'error');
    } finally {
      setLoading(false);
    }
  }, [activeTab, selectedCategory, searchKeyword, currentStudent.id, showToast]);

  useEffect(() => {
    let ignore = false;
    async function initFetch() {
      try {
        let data = [];
        if (activeTab === 'my-listings') {
          data = await listingApi.getMyListings(currentStudent.id);
        } else if (activeTab === 'all') {
          data = await listingApi.getAllListings('', selectedCategory, searchKeyword);
        } else {
          data = await listingApi.getActiveListings(selectedCategory, searchKeyword);
        }
        if (!ignore) {
          setListings(data);
        }
      } catch (err) {
        if (!ignore) {
          showToast(err.message || 'Could not load listings from server.', 'error');
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    initFetch();

    return () => {
      ignore = true;
    };
  }, [activeTab, selectedCategory, searchKeyword, currentStudent.id, showToast]);

  // Derived filtered listings for condition
  const displayedListings = listings.filter((item) => {
    if (conditionFilter !== 'ALL' && item.condition !== conditionFilter) {
      return false;
    }
    return true;
  });

  // Calculate summary stats
  const activeCount = listings.filter((l) => l.status === 'ACTIVE' && !l.expired).length;
  const myCount = listings.filter((l) => l.sellerId === currentStudent.id).length;

  return (
    <div className="app-shell">
      {/* Top Navigation */}
      <Navbar
        currentStudent={currentStudent}
        onStudentChange={(newStudent) => {
          setCurrentStudent(newStudent);
          showToast(`Switched active student to ${newStudent.name} (${newStudent.id})`, 'info');
        }}
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          setSelectedCategory('All');
        }}
        onCreateClick={() => setIsCreateOpen(true)}
        stats={{ activeCount, myCount }}
      />

      {/* Main Content Area */}
      <main className="main-content">
        {/* Security & Authentication limitation explanation */}
        <SecurityBanner currentStudent={currentStudent} />

        {/* Hero Section */}
        <section className="hero-section">
          <div className="hero-content">
            <span className="hero-badge">🎪 University Flea Market & Auction</span>
            <h1 className="hero-title">
              Campus Flea Market & Student Auctions
            </h1>
            <p className="hero-subtitle">
              Browse pre-loved textbooks, dorm essentials, electronics, and campus bikes directly from classmates. Place bids on active lots before the hammer falls.
            </p>

            <div className="hero-cta-row">
              <button className="btn-primary btn-lg" onClick={() => setIsCreateOpen(true)}>
                <span>+ Consign / List an Item</span>
              </button>
              <button
                className="btn-outline btn-lg"
                onClick={() => {
                  setActiveTab('my-listings');
                }}
              >
                <span>📦 My Consigned Lots ({myCount})</span>
              </button>
            </div>
          </div>
        </section>
        {/* Search & Category Filter Controls */}
        <section className="filter-panel">
          <div className="search-bar-row">
            <div className="search-input-wrapper">
              <span className="search-icon">🔍</span>
              <input
                type="text"
                className="search-input"
                placeholder="Search by title or description (e.g. MacBook, Calculus, Fridge)..."
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
              />
              {searchKeyword && (
                <button className="search-clear-btn" onClick={() => setSearchKeyword('')}>
                  ✕
                </button>
              )}
            </div>

            <div className="condition-select-wrapper">
              <label htmlFor="condition-filter" className="filter-label">Condition:</label>
              <select
                id="condition-filter"
                className="form-select filter-select"
                value={conditionFilter}
                onChange={(e) => setConditionFilter(e.target.value)}
              >
                <option value="ALL">All Conditions</option>
                <option value="NEW">Brand New</option>
                <option value="LIKE_NEW">Like New</option>
                <option value="GOOD">Good</option>
                <option value="FAIR">Fair</option>
                <option value="POOR">Poor</option>
              </select>
            </div>
          </div>

          {/* Category Chips */}
          <div className="category-chips-scroll">
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                className={`category-chip ${selectedCategory === cat ? 'active' : ''}`}
                onClick={() => setSelectedCategory(cat)}
              >
                <span className="chip-icon">{CATEGORY_ICONS[cat] || '🏷️'}</span>
                <span>{cat}</span>
              </button>
            ))}
          </div>
        </section>

        {/* Listings Section Header */}
        <section className="listings-header-row">
          <div>
            <h2 className="section-title">
              {activeTab === 'my-listings'
                ? `My Consigned Lots — ${currentStudent.name}`
                : activeTab === 'all'
                ? 'Catalog Archive & Cancelled Lots'
                : 'Live Flea Market Lots'}
            </h2>
            <p className="section-caption">
              {activeTab === 'my-listings'
                ? `Showing lots consigned under Student ID: ${currentStudent.id}. You hold full permission to edit or withdraw these items.`
                : activeTab === 'all'
                ? 'Complete registry of campus lots, including active auctions, expired listings, and cancelled entries retained in MySQL.'
                : 'Browse authentic campus gear. Place your bid before each lot closes.'}
            </p>
          </div>

          <div className="results-counter">
            Showing <strong>{displayedListings.length}</strong> items
          </div>
        </section>

        {/* Listings Grid */}
        {loading ? (
          <div className="loading-state">
            <div className="spinner-large"></div>
            <p>Fetching listings from CampusBid backend...</p>
          </div>
        ) : displayedListings.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📦</div>
            <h3 className="empty-title">No listings found</h3>
            <p className="empty-desc">
              {activeTab === 'my-listings'
                ? `You haven't listed any items under ${currentStudent.name} (${currentStudent.id}) yet.`
                : 'No items match your selected filters or search keyword.'}
            </p>
            <button className="btn-primary" onClick={() => setIsCreateOpen(true)}>
              + Create a Listing
            </button>
          </div>
        ) : (
          <div className="listings-grid">
            {displayedListings.map((listing) => (
              <ListingCard
                key={listing.id}
                listing={listing}
                currentStudent={currentStudent}
                onViewDetails={(item) => setDetailTarget(item)}
                onEdit={(item) => setEditTarget(item)}
                onCancel={(item) => setCancelTarget(item)}
              />
            ))}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="site-footer">
        <div className="footer-inner">
          <div className="footer-brand">
            <span className="footer-logo">🏷️ CampusBid</span>
            <span className="footer-copy">Student Auction & Marketplace Platform • Powered by React + Spring Boot + MySQL</span>
          </div>
          <div className="footer-note">
            REST API: <code>http://localhost:8080/api/listings</code>
          </div>
        </div>
      </footer>

      {/* Modals */}
      <CreateListingModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        currentStudent={currentStudent}
        onSuccess={() => {
          loadListings();
        }}
        showToast={showToast}
      />

      <EditListingModal
        isOpen={!!editTarget}
        onClose={() => setEditTarget(null)}
        listing={editTarget}
        currentStudent={currentStudent}
        onSuccess={() => {
          loadListings();
        }}
        showToast={showToast}
      />

      <CancelModal
        isOpen={!!cancelTarget}
        onClose={() => setCancelTarget(null)}
        listing={cancelTarget}
        currentStudent={currentStudent}
        onSuccess={() => {
          loadListings();
        }}
        showToast={showToast}
      />

      <ListingDetailModal
        isOpen={!!detailTarget}
        onClose={() => setDetailTarget(null)}
        listing={detailTarget}
        currentStudent={currentStudent}
        onEdit={(item) => setEditTarget(item)}
        onCancel={(item) => setCancelTarget(item)}
      />

      {/* Toast Alert */}
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
