import { useState, useEffect } from 'react';
import { CATEGORY_ICONS, CONDITION_COLORS, calculateTimeRemaining, formatDate } from '../constants';

export default function ListingCard({
  listing,
  currentStudent,
  onViewDetails,
  onEdit,
  onCancel,
}) {
  const isOwner = listing.sellerId === currentStudent.id;
  const isCancelled = listing.status === 'CANCELLED';

  // Live countdown timer logic
  const [countdown, setCountdown] = useState(() => calculateTimeRemaining(listing.closingTime));

  useEffect(() => {
    const updateCountdown = () => {
      setCountdown(calculateTimeRemaining(listing.closingTime));
    };
    updateCountdown();
    const timer = setInterval(updateCountdown, 1000);
    return () => clearInterval(timer);
  }, [listing.closingTime]);

  const isClosed = listing.status === 'ENDED' || listing.expired || countdown.isExpired;

  const photos = listing.photoUrls && listing.photoUrls.length > 0
    ? listing.photoUrls
    : ['https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80'];

  const coverPhoto = photos[0];
  const conditionMeta = CONDITION_COLORS[listing.condition] || { label: listing.condition, class: 'badge-blue' };
  const categoryIcon = CATEGORY_ICONS[listing.category] || '📦';

  const leadingBid = listing.currentLeadingBid != null
    ? listing.currentLeadingBid
    : (listing.currentPrice != null ? listing.currentPrice : listing.startingPrice);
  const totalBids = listing.totalBids || (listing.bidHistory ? listing.bidHistory.length : 0);
  const hasBids = totalBids > 0;

  return (
    <article className={`listing-card ${isCancelled ? 'card-cancelled' : ''} ${isClosed && !isCancelled ? 'card-closed' : ''}`}>
      {/* Media Header */}
      <div className="card-media" onClick={() => onViewDetails(listing)}>
        <img
          src={coverPhoto}
          alt={listing.title}
          className="card-image"
          loading="lazy"
          onError={(e) => {
            e.target.onerror = null;
            e.target.src = 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80';
          }}
        />

        {/* Lot tag & Photo Count */}
        <div className="card-top-tags">
          <span className="lot-badge">
            LOT #{String(listing.id).padStart(3, '0')}
          </span>
          {photos.length > 1 && (
            <span className="photo-count-badge">
              📷 {photos.length}
            </span>
          )}
        </div>

        {/* Status Overlay Badge */}
        {isCancelled ? (
          <span className="status-pill status-cancelled">🚫 Cancelled</span>
        ) : isClosed ? (
          <span className="status-pill status-ended">⌛ Auction Closed</span>
        ) : (
          <span className="status-pill status-active">
            ⏳ {countdown.text}
          </span>
        )}

        {/* Category Pill */}
        <span className="category-pill">
          {categoryIcon} {listing.category}
        </span>
      </div>

      {/* Card Body */}
      <div className="card-body">
        {/* Condition & Seller header */}
        <div className="card-meta-row">
          <span className={`condition-tag ${conditionMeta.class}`}>
            {conditionMeta.label}
          </span>
          <span className="seller-badge" title={`Seller: ${listing.sellerName || listing.sellerId} (${listing.sellerId})`}>
            {isOwner ? (
              <span className="owner-tag">👤 You ({listing.sellerId})</span>
            ) : (
              <span>👤 {listing.sellerName || listing.sellerId}</span>
            )}
          </span>
        </div>

        {/* Title */}
        <h3 className="card-title" onClick={() => onViewDetails(listing)} title={listing.title}>
          {listing.title}
        </h3>

        {/* Description Snippet */}
        <p className="card-description">
          {listing.description}
        </p>

        {/* Pricing & Bidding Specs */}
        <div className="pricing-grid">
          <div className="price-item">
            <span className="price-label">
              {hasBids ? 'Leading Bid' : 'Starting Bid'}
            </span>
            <span className="price-value" title={`Current Leading Bid: $${Number(leadingBid).toFixed(2)}`}>
              ${Number(leadingBid).toFixed(2)}
            </span>
            <span className="price-subnote">
              {hasBids ? `🔥 ${totalBids} ${totalBids === 1 ? 'bid' : 'bids'}` : 'No bids yet'}
            </span>
          </div>

          <div className="price-item price-item-secondary">
            <div className="sub-price-row">
              <span className="price-label">Start Price</span>
              <span className="price-subvalue">${Number(listing.startingPrice).toFixed(2)}</span>
            </div>
            <div className="sub-price-row">
              <span className="price-label">Min Increment</span>
              <span className="price-increment">+${Number(listing.minBidIncrement).toFixed(2)}</span>
            </div>
          </div>
        </div>

        {/* Closing Time Info */}
        <div className="closing-meta">
          <span className="closing-label">Closes:</span>
          <span className="closing-date">
            {formatDate(listing.closingTime)}
          </span>
        </div>

        {/* Action Controls */}
        <div className="card-actions">
          <button
            className="btn-secondary btn-sm"
            onClick={() => onViewDetails(listing)}
            title="View item photos, details, and bid history"
          >
            Details & History
          </button>

          {isOwner ? (
            <>
              <button
                className="btn-edit btn-sm"
                onClick={() => onEdit(listing)}
                disabled={isCancelled || isClosed}
                title={isCancelled ? 'Cancelled listings cannot be edited' : isClosed ? 'Closed auctions cannot be edited' : 'Edit your listing'}
              >
                ✏️ Edit
              </button>
              <button
                className="btn-cancel btn-sm"
                onClick={() => onCancel(listing)}
                disabled={isCancelled || isClosed}
                title={isCancelled ? 'Listing is already cancelled' : isClosed ? 'Closed auctions cannot be cancelled' : 'Cancel this listing'}
              >
                {isCancelled ? 'Cancelled' : isClosed ? 'Closed' : '🛑 Cancel'}
              </button>
            </>
          ) : (
            <div className="locked-pill" title="Security: You cannot edit or cancel listings owned by other students">
              🔒 Seller Only
            </div>
          )}
        </div>
      </div>
    </article>
  );
}
