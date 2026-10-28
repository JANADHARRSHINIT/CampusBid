import { useState, useEffect } from 'react';
import { CONDITION_COLORS, CATEGORY_ICONS, calculateTimeRemaining, formatFullDate } from '../constants';

export default function ListingDetailModal({
  isOpen,
  onClose,
  listing,
  currentStudent,
  onEdit,
  onCancel,
}) {
  if (!isOpen || !listing) return null;

  return (
    <ListingDetailView
      key={listing.id}
      listing={listing}
      onClose={onClose}
      currentStudent={currentStudent}
      onEdit={onEdit}
      onCancel={onCancel}
    />
  );
}

function ListingDetailView({
  listing,
  onClose,
  currentStudent,
  onEdit,
  onCancel,
}) {
  const [activePhotoIdx, setActivePhotoIdx] = useState(0);

  // Live countdown timer in detail view
  const [countdown, setCountdown] = useState(() => calculateTimeRemaining(listing.closingTime));

  useEffect(() => {
    const updateCountdown = () => {
      setCountdown(calculateTimeRemaining(listing.closingTime));
    };
    updateCountdown();
    const timer = setInterval(updateCountdown, 1000);
    return () => clearInterval(timer);
  }, [listing.closingTime]);

  const photos = listing.photoUrls && listing.photoUrls.length > 0
    ? listing.photoUrls
    : ['https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80'];

  const isOwner = listing.sellerId === currentStudent.id;
  const isCancelled = listing.status === 'CANCELLED';
  const isClosed = listing.status === 'ENDED' || listing.expired || countdown.isExpired;
  const conditionMeta = CONDITION_COLORS[listing.condition] || { label: listing.condition, class: 'badge-blue' };

  const leadingBid = listing.currentLeadingBid != null
    ? listing.currentLeadingBid
    : (listing.currentPrice != null ? listing.currentPrice : listing.startingPrice);

  const bids = listing.bidHistory && listing.bidHistory.length > 0
    ? [...listing.bidHistory].sort((a, b) => Number(b.amount) - Number(a.amount))
    : [];
  const hasBids = bids.length > 0;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-container modal-xl" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="detail-header-tags">
            <span className="lot-badge">LOT #{String(listing.id).padStart(3, '0')}</span>
            <span className="category-pill">
              {CATEGORY_ICONS[listing.category] || '📦'} {listing.category}
            </span>
            <span className={`condition-tag ${conditionMeta.class}`}>
              {conditionMeta.label}
            </span>
            {isCancelled ? (
              <span className="status-pill status-cancelled">🚫 Cancelled</span>
            ) : isClosed ? (
              <span className="status-pill status-ended">⌛ Auction Closed</span>
            ) : (
              <span className="status-pill status-active">⏳ {countdown.text}</span>
            )}
          </div>
          <button className="modal-close-btn" onClick={onClose}>✕</button>
        </div>

        <div className="detail-layout">
          {/* Left Column: Gallery */}
          <div className="detail-gallery">
            <div className="main-photo-wrapper">
              <img
                src={photos[activePhotoIdx]}
                alt={listing.title}
                className="main-photo"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80';
                }}
              />
            </div>

            {photos.length > 1 && (
              <div className="thumb-strip">
                {photos.map((p, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className={`thumb-button ${idx === activePhotoIdx ? 'active' : ''}`}
                    onClick={() => setActivePhotoIdx(idx)}
                  >
                    <img src={p} alt={`Thumbnail ${idx + 1}`} />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Right Column: Listing Details & Pricing */}
          <div className="detail-content">
            <h1 className="detail-title">{listing.title}</h1>

            {/* Auction Pricing & Leading Bid Box */}
            <div className="detail-price-box">
              <div className="price-item detail-leading-bid-box">
                <span className="price-label">
                  {hasBids ? 'Current Leading Bid' : 'Starting Price'}
                </span>
                <span className="price-large">
                  ${Number(leadingBid).toFixed(2)}
                </span>
                {hasBids ? (
                  <span className="bid-status-note">🔥 Leading bid ({bids.length} {bids.length === 1 ? 'bid' : 'bids'})</span>
                ) : (
                  <span className="bid-status-note">No bids placed yet</span>
                )}
              </div>

              <div className="price-item">
                <span className="price-label">Starting Price</span>
                <span className="price-secondary-val">${Number(listing.startingPrice).toFixed(2)}</span>
              </div>

              <div className="price-item">
                <span className="price-label">Min Bid Increment</span>
                <span className="price-increment-large">+${Number(listing.minBidIncrement).toFixed(2)}</span>
              </div>
            </div>

            {/* Auction Timing & Deadline */}
            <div className="detail-timing-box">
              <div className="timing-row">
                <span className="timing-label">Closing Date & Time:</span>
                <span className="timing-val">{formatFullDate(listing.closingTime)}</span>
              </div>
              <div className="timing-row">
                <span className="timing-label">Countdown / Deadline:</span>
                <span className={`timing-val ${isClosed ? 'text-closed' : 'text-active-countdown'}`}>
                  {isCancelled ? 'Cancelled' : isClosed ? 'Auction Closed' : `⏳ ${countdown.text}`}
                </span>
              </div>
              <div className="timing-row">
                <span className="timing-label">Status:</span>
                <span className="timing-val">
                  {isCancelled ? 'Cancelled (Retained in Database)' : isClosed ? 'Auction Closed' : 'Open for Bidding'}
                </span>
              </div>
            </div>

            {/* Description */}
            <div className="detail-section">
              <h3 className="section-heading">Description</h3>
              <p className="detail-description-text">{listing.description}</p>
            </div>

            {/* Public Bid History Section */}
            <div className="detail-section bid-history-section">
              <div className="section-header-flex">
                <h3 className="section-heading">📜 Public Bid History</h3>
                <span className="bid-count-pill">{bids.length} {bids.length === 1 ? 'Bid' : 'Bids'}</span>
              </div>

              {bids.length > 0 ? (
                <div className="bid-history-table-wrapper">
                  <table className="bid-history-table">
                    <thead>
                      <tr>
                        <th>Rank</th>
                        <th>Bidder</th>
                        <th>Bid Amount</th>
                        <th>Time</th>
                      </tr>
                    </thead>
                    <tbody>
                      {bids.map((bid, idx) => (
                        <tr key={idx} className={idx === 0 ? 'leading-bid-row' : ''}>
                          <td>
                            {idx === 0 ? (
                              <span className="leading-rank-tag">🏆 Leading</span>
                            ) : (
                              <span className="rank-tag">#{idx + 1}</span>
                            )}
                          </td>
                          <td className="bidder-cell">
                            <span className="bidder-icon">👤</span>
                            <span className="bidder-name">{bid.bidderName || bid.bidderId}</span>
                          </td>
                          <td className="bid-amount-cell">
                            ${Number(bid.amount).toFixed(2)}
                          </td>
                          <td className="bid-time-cell">
                            {formatFullDate(bid.bidTime)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="no-bids-message">
                  <span className="no-bids-icon">🏷️</span>
                  <div>
                    <p className="no-bids-title">No bids placed yet</p>
                    <p className="no-bids-desc">
                      Bidding starts at <strong>${Number(listing.startingPrice).toFixed(2)}</strong> with minimum bid increments of <strong>+${Number(listing.minBidIncrement).toFixed(2)}</strong>.
                    </p>
                  </div>
                </div>
              )}
            </div>

            {/* Seller Info Card */}
            <div className="detail-seller-card">
              <div className="seller-avatar">🎓</div>
              <div className="seller-info">
                <span className="seller-name">{listing.sellerName || listing.sellerId}</span>
                <span className="seller-id">Student ID: <code>{listing.sellerId}</code></span>
                {listing.sellerEmail && (
                  <span className="seller-email">✉️ {listing.sellerEmail}</span>
                )}
              </div>
              {isOwner && <span className="owner-badge">You own this</span>}
            </div>

            {/* Actions */}
            <div className="detail-actions">
              {isOwner ? (
                <>
                  <button
                    className="btn-edit"
                    onClick={() => {
                      onClose();
                      onEdit(listing);
                    }}
                    disabled={isCancelled || isClosed}
                    title={isCancelled ? 'Cancelled listings cannot be edited' : isClosed ? 'Closed auctions cannot be edited' : 'Edit listing'}
                  >
                    ✏️ Edit Listing
                  </button>
                  <button
                    className="btn-cancel"
                    onClick={() => {
                      onClose();
                      onCancel(listing);
                    }}
                    disabled={isCancelled || isClosed}
                    title={isCancelled ? 'Already cancelled' : isClosed ? 'Closed auctions cannot be cancelled' : 'Cancel listing'}
                  >
                    {isCancelled ? 'Already Cancelled' : isClosed ? 'Auction Closed' : '🛑 Cancel Listing'}
                  </button>
                </>
              ) : (
                <div className="foreign-seller-notice">
                  <span>🔒 Only <strong>{listing.sellerName || listing.sellerId}</strong> can edit or cancel this listing.</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
