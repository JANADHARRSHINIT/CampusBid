import { useState, useEffect, useCallback } from 'react';
import Navbar from './components/Navbar';
import ListingCard from './components/ListingCard';
import CreateListingModal from './components/CreateListingModal';
import EditListingModal from './components/EditListingModal';
import CancelModal from './components/CancelModal';
import ListingDetailModal from './components/ListingDetailModal';
import CopilotDrawer from './components/CopilotDrawer';
import NotificationModal from './components/NotificationModal';
import Toast from './components/Toast';
import { listingApi } from './api/listingApi';
import { notificationApi } from './api/notificationApi';
import { CAMPUS_STUDENTS, CATEGORIES, CATEGORY_ICONS } from './constants';
import './App.css';

export default function App() {
  const [currentStudent, setCurrentStudent] = useState(CAMPUS_STUDENTS[0]);
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('active'); // 'active' | 'decisions' | 'won' | 'my-listings' | 'all'

  // Sale tracking stats & Notifications
  const [pendingDecisions, setPendingDecisions] = useState([]);
  const [wonListings, setWonListings] = useState([]);
  const [unreadNotifCount, setUnreadNotifCount] = useState(0);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);

  // Search & Filter state
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [debouncedKeyword, setDebouncedKeyword] = useState('');
  const [conditionFilter, setConditionFilter] = useState('ALL');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [closingSoon, setClosingSoon] = useState(false);
  const [sortBy, setSortBy] = useState('newest');

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [detailTarget, setDetailTarget] = useState(null);

  // AI Copilot state
  const [isCopilotOpen, setIsCopilotOpen] = useState(false);
  const [copilotContextListing, setCopilotContextListing] = useState(null);

  // Toast notification
  const [toast, setToast] = useState(null);

  const showToast = useCallback((message, type = 'info') => {
    setToast({ message, type });
  }, []);

  // Debounce search keyword (300ms) to ensure smooth typing without API thrashing
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedKeyword(searchKeyword);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchKeyword]);

  // Check if any filter is active
  const isFilterActive = selectedCategory !== 'All' ||
    conditionFilter !== 'ALL' ||
    Boolean(searchKeyword.trim()) ||
    minPrice !== '' ||
    maxPrice !== '' ||
    closingSoon ||
    sortBy !== 'newest';

  // Refresh trigger for modal completion
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const reloadListings = useCallback(() => {
    setRefreshTrigger((prev) => prev + 1);
  }, []);

  // Clear all filters action
  const clearAllFilters = useCallback(() => {
    setSelectedCategory('All');
    setConditionFilter('ALL');
    setSearchKeyword('');
    setMinPrice('');
    setMaxPrice('');
    setClosingSoon(false);
    setSortBy('newest');
  }, []);

  // Sync sale management stats (pending decisions and won listings)
  useEffect(() => {
    let ignore = false;
    async function loadSaleStats() {
      try {
        const [pending, won, unread] = await Promise.all([
          listingApi.getPendingDecisions(currentStudent.id),
          listingApi.getWonListings(currentStudent.id),
          notificationApi.getUnreadCount(currentStudent.id),
        ]);
        if (!ignore) {
          setPendingDecisions(pending || []);
          setWonListings(won || []);
          setUnreadNotifCount(unread || 0);
        }
      } catch {
        // fail gracefully
      }
    }
    loadSaleStats();
    return () => {
      ignore = true;
    };
  }, [currentStudent.id, refreshTrigger]);

  // Fetch listings based on current tab, multi-criteria filters, and keyword search
  useEffect(() => {
    let ignore = false;

    async function fetchData() {
      try {
        setLoading(true);
        let data = [];

        if (activeTab === 'decisions') {
          // Closed lots consigned by current student awaiting their decision
          data = await listingApi.getPendingDecisions(currentStudent.id);
        } else if (activeTab === 'won') {
          // Lots won by current student
          data = await listingApi.getWonListings(currentStudent.id);
        } else if (activeTab === 'my-listings') {
          const raw = await listingApi.getMyListings(currentStudent.id);
          // Client-side filter for seller's personal lots
          data = raw.filter((item) => {
            if (selectedCategory !== 'All' && item.category !== selectedCategory) return false;
            if (conditionFilter !== 'ALL' && item.condition !== conditionFilter) return false;
            if (minPrice !== '' && Number(item.startingPrice) < Number(minPrice)) return false;
            if (maxPrice !== '' && Number(item.startingPrice) > Number(maxPrice)) return false;
            if (closingSoon) {
              const closing = new Date(item.closingTime).getTime();
              const now = Date.now();
              if (closing <= now || closing > now + 24 * 60 * 60 * 1000) return false;
            }
            if (debouncedKeyword.trim()) {
              const kw = debouncedKeyword.trim().toLowerCase();
              const inTitle = item.title && item.title.toLowerCase().includes(kw);
              const inDesc = item.description && item.description.toLowerCase().includes(kw);
              if (!inTitle && !inDesc) return false;
            }
            return true;
          });
        } else {
          const filterParams = {
            category: selectedCategory,
            condition: conditionFilter,
            minPrice: minPrice !== '' ? minPrice : undefined,
            maxPrice: maxPrice !== '' ? maxPrice : undefined,
            closingSoon: closingSoon || undefined,
            keyword: debouncedKeyword,
            sortBy: sortBy !== 'newest' ? sortBy : undefined,
          };

          if (activeTab === 'all') {
            data = await listingApi.getAllListings(filterParams);
          } else {
            // 'active'
            data = await listingApi.getActiveListings(filterParams);
          }
        }
        if (!ignore) {
          setListings(data);
          setLoading(false);
        }
      } catch (err) {
        if (!ignore) {
          showToast(err.message || 'Could not load listings from server.', 'error');
          setLoading(false);
        }
      }
    }

    fetchData();

    return () => {
      ignore = true;
    };
  }, [
    activeTab,
    selectedCategory,
    conditionFilter,
    minPrice,
    maxPrice,
    closingSoon,
    sortBy,
    debouncedKeyword,
    currentStudent.id,
    refreshTrigger,
    showToast,
  ]);

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
          showToast(`Active account: ${newStudent.name} (${newStudent.id})`, 'info');
        }}
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          clearAllFilters();
        }}
        onCreateClick={() => setIsCreateOpen(true)}
        onNotificationsClick={() => setIsNotificationsOpen(true)}
        unreadNotifCount={unreadNotifCount}
        stats={{
          activeCount,
          myCount,
          pendingCount: pendingDecisions.length,
          wonCount: wonListings.length,
        }}
      />

      {/* Main Content Area */}
      <main className="main-content">
        {/* Seller Pending Decision Alert Banner */}
        {pendingDecisions.length > 0 && activeTab !== 'decisions' && (
          <div className="seller-pending-alert-banner">
            <div className="alert-content">
              <span className="alert-badge">⚡ Action Required</span>
              <span className="alert-text">
                You have <strong>{pendingDecisions.length}</strong> closed {pendingDecisions.length === 1 ? 'lot' : 'lots'} awaiting your <strong>APPROVE</strong> or <strong>REJECT</strong> decision.
              </span>
            </div>
            <button
              type="button"
              className="btn-review-pending"
              onClick={() => setActiveTab('decisions')}
            >
              Review Pending Decisions →
            </button>
          </div>
        )}

        {/* Hero Section */}
        <section className="hero-section">
          <div className="hero-content">
            <span className="hero-badge">🎪 University Flea Market & Auction</span>
            <h1 className="hero-title">
              Campus Flea Market & Student Auctions
            </h1>
            <p className="hero-subtitle">
              Browse pre-loved textbooks, dorm essentials, electronics, and campus bikes directly from classmates. Search items by keyword, filter by condition or price, and bid on active lots before the hammer falls.
            </p>

            <div className="hero-cta-row">
              <button className="btn-primary btn-lg" onClick={() => setIsCreateOpen(true)}>
                <span>+ Create Listing</span>
              </button>
              <button
                className="btn-outline btn-lg"
                onClick={() => {
                  setActiveTab('my-listings');
                }}
              >
                <span>📦 My Listings ({myCount})</span>
              </button>
            </div>
          </div>
        </section>

        {/* Search & Multi-Criteria Filter Controls */}
        <section className="filter-panel">
          {/* Row 1: Search Bar & Sort Dropdown */}
          <div className="search-bar-row">
            <div className="search-input-wrapper">
              <span className="search-icon">🔍</span>
              <input
                type="text"
                className="search-input"
                placeholder="Search auctions by item title or description (e.g. MacBook, Calculus, Fridge)..."
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
              />
              {searchKeyword && (
                <button
                  type="button"
                  className="search-clear-btn"
                  onClick={() => setSearchKeyword('')}
                  title="Clear search keyword"
                >
                  ✕
                </button>
              )}
            </div>

            {/* Sort Selector */}
            <div className="sort-select-wrapper">
              <label htmlFor="sort-filter" className="filter-label">Sort:</label>
              <select
                id="sort-filter"
                className="filter-select"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
              >
                <option value="newest">✨ Newest First</option>
                <option value="closingSoon">⏳ Closing Soonest</option>
                <option value="priceLow">💰 Price: Low to High</option>
                <option value="priceHigh">💎 Price: High to Low</option>
              </select>
            </div>
          </div>

          {/* Row 2: Condition, Price Range, Closing Soon, & Clear Button */}
          <div className="filter-secondary-row">
            {/* Condition Dropdown */}
            <div className="condition-select-wrapper">
              <label htmlFor="condition-filter" className="filter-label">Condition:</label>
              <select
                id="condition-filter"
                className="filter-select"
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

            {/* Price Range Inputs */}
            <div className="price-filter-wrapper">
              <span className="filter-label">Price Range:</span>
              <div className="price-inputs-group">
                <input
                  type="number"
                  min="0"
                  step="1"
                  placeholder="Min $"
                  className="price-num-input"
                  value={minPrice}
                  onChange={(e) => setMinPrice(e.target.value)}
                  title="Minimum price"
                />
                <span className="price-divider">–</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  placeholder="Max $"
                  className="price-num-input"
                  value={maxPrice}
                  onChange={(e) => setMaxPrice(e.target.value)}
                  title="Maximum price"
                />
              </div>
            </div>

            {/* Closing Soon Toggle */}
            <button
              type="button"
              className={`closing-soon-toggle ${closingSoon ? 'active' : ''}`}
              onClick={() => setClosingSoon((prev) => !prev)}
              title="Filter lots ending within the next 24 hours"
            >
              <span>⏳ Closing Soon (&lt;24h)</span>
              {closingSoon && <span>✓</span>}
            </button>

            {/* Clear All Filters Button */}
            {isFilterActive && (
              <button
                type="button"
                className="btn-clear-filters"
                onClick={clearAllFilters}
                title="Reset all filters and search keyword"
              >
                🔄 Clear All Filters
              </button>
            )}
          </div>

          {/* Row 3: Active Filters Summary Tag Row */}
          {isFilterActive && (
            <div className="active-filters-bar">
              <span className="active-filters-title">Active Filters:</span>
              {searchKeyword.trim() && (
                <span className="active-filter-tag">
                  Keyword: &ldquo;{searchKeyword.trim()}&rdquo;
                  <button type="button" className="remove-filter-btn" onClick={() => setSearchKeyword('')}>✕</button>
                </span>
              )}
              {selectedCategory !== 'All' && (
                <span className="active-filter-tag">
                  Category: {selectedCategory}
                  <button type="button" className="remove-filter-btn" onClick={() => setSelectedCategory('All')}>✕</button>
                </span>
              )}
              {conditionFilter !== 'ALL' && (
                <span className="active-filter-tag">
                  Condition: {conditionFilter}
                  <button type="button" className="remove-filter-btn" onClick={() => setConditionFilter('ALL')}>✕</button>
                </span>
              )}
              {(minPrice !== '' || maxPrice !== '') && (
                <span className="active-filter-tag">
                  Price: ${minPrice || '0'} – ${maxPrice || '∞'}
                  <button type="button" className="remove-filter-btn" onClick={() => { setMinPrice(''); setMaxPrice(''); }}>✕</button>
                </span>
              )}
              {closingSoon && (
                <span className="active-filter-tag">
                  Closing Soon (&lt;24h)
                  <button type="button" className="remove-filter-btn" onClick={() => setClosingSoon(false)}>✕</button>
                </span>
              )}
              {sortBy !== 'newest' && (
                <span className="active-filter-tag">
                  Sort: {sortBy}
                  <button type="button" className="remove-filter-btn" onClick={() => setSortBy('newest')}>✕</button>
                </span>
              )}
            </div>
          )}

          {/* Row 4: Category Chips */}
          <div className="category-chips-scroll">
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                type="button"
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
              {activeTab === 'decisions'
                ? `⚡ Seller Decision Station — ${currentStudent.name}`
                : activeTab === 'won'
                ? `🏆 Won Auctions & Handover — ${currentStudent.name}`
                : activeTab === 'my-listings'
                ? `My Consigned Lots — ${currentStudent.name}`
                : activeTab === 'all'
                ? 'Catalog Archive & Cancelled Lots'
                : 'Live Flea Market Lots'}
            </h2>
            <p className="section-caption">
              {activeTab === 'decisions'
                ? `These lots reached their deadline with valid bids. Review each winning bid below and either APPROVE to reserve the lot or REJECT.`
                : activeTab === 'won'
                ? `Auctions where your bid won. Review seller approval status and coordinate physical campus exchange.`
                : activeTab === 'my-listings'
                ? `Items you have listed on CampusBid. You can edit active details or withdraw lots before bidding closes.`
                : activeTab === 'all'
                ? 'Complete directory of campus lots, including active auctions, completed sales, and past listings.'
                : 'Browse authentic campus gear. Place your bid before each lot closes.'}
            </p>
          </div>

          <div className="results-counter">
            Showing <strong>{listings.length}</strong> items
          </div>
        </section>

        {/* Listings Grid */}
        {loading ? (
          <div className="loading-state">
            <div className="spinner-large"></div>
            <p>Loading campus auctions...</p>
          </div>
        ) : listings.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">
              {activeTab === 'decisions' ? '⚡' : activeTab === 'won' ? '🏆' : '📦'}
            </div>
            <h3 className="empty-title">
              {activeTab === 'decisions'
                ? 'No pending decisions'
                : activeTab === 'won'
                ? 'No won auctions yet'
                : 'No listings found'}
            </h3>
            <p className="empty-desc">
              {activeTab === 'decisions'
                ? `You have no closed auctions awaiting decision. When your auction closes with bids, it will appear here for review.`
                : activeTab === 'won'
                ? `You haven't won any auctions yet. Place leading bids on active campus lots and wait for auction closing.`
                : activeTab === 'my-listings'
                ? `You haven't listed any items for auction yet.`
                : isFilterActive
                ? 'No items match your active search keyword and filter criteria. Try clearing some filters or searching with different terms.'
                : 'No auction items currently available in this category.'}
            </p>
            <div className="empty-actions" style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', marginTop: '1rem' }}>
              {isFilterActive && (
                <button type="button" className="btn-secondary" onClick={clearAllFilters}>
                  🔄 Clear All Filters
                </button>
              )}
              {activeTab === 'decisions' || activeTab === 'won' ? (
                <button type="button" className="btn-primary" onClick={() => setActiveTab('active')}>
                  🔥 Browse Active Auctions
                </button>
              ) : (
                <button type="button" className="btn-primary" onClick={() => setIsCreateOpen(true)}>
                  + Create Listing
                </button>
              )}
            </div>
          </div>
        ) : (
          <div className="listings-grid">
            {listings.map((listing) => (
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
            <span className="footer-copy">Campus Marketplace & Student Flea Market Auctions</span>
          </div>
          <div className="footer-note">
            Verified Student Profiles • Safe In-Person Handover
          </div>
        </div>
      </footer>

      {/* Modals */}
      <CreateListingModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        currentStudent={currentStudent}
        onSuccess={() => {
          reloadListings();
        }}
        showToast={showToast}
      />

      <EditListingModal
        isOpen={!!editTarget}
        onClose={() => setEditTarget(null)}
        listing={editTarget}
        currentStudent={currentStudent}
        onSuccess={() => {
          reloadListings();
        }}
        showToast={showToast}
      />

      <CancelModal
        isOpen={!!cancelTarget}
        onClose={() => setCancelTarget(null)}
        listing={cancelTarget}
        currentStudent={currentStudent}
        onSuccess={() => {
          reloadListings();
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
        onBidPlaced={() => {
          reloadListings();
        }}
        onAskCopilot={(item) => {
          setCopilotContextListing(item);
          setIsCopilotOpen(true);
        }}
      />

      {/* AI Auction Copilot Floating Assistant & Drawer */}
      <CopilotDrawer
        isOpen={isCopilotOpen}
        onToggle={() => setIsCopilotOpen((prev) => !prev)}
        onClose={() => setIsCopilotOpen(false)}
        currentStudent={currentStudent}
        contextListing={copilotContextListing}
        onClearContext={() => setCopilotContextListing(null)}
        onViewLot={(item) => {
          setDetailTarget(item);
          setCopilotContextListing(item);
        }}
        onBidConfirmed={(result) => {
          reloadListings();
          showToast(`Bid placed successfully via Copilot!`, 'success');
        }}
      />

      {/* Student Notifications Modal */}
      <NotificationModal
        isOpen={isNotificationsOpen}
        onClose={() => setIsNotificationsOpen(false)}
        currentStudent={currentStudent}
        onViewListing={async (lotId) => {
          try {
            const lot = await listingApi.getListingById(lotId);
            if (lot) {
              setDetailTarget(lot);
            }
          } catch {
            // ignore
          }
        }}
        onNotificationsUpdated={() => {
          notificationApi.getUnreadCount(currentStudent.id).then((count) => {
            setUnreadNotifCount(count || 0);
          }).catch(() => {});
        }}
      />

      {/* Toast Alert */}
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
