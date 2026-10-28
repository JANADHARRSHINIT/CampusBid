import { useState, useEffect } from 'react';
import { CATEGORY_ICONS, CONDITION_COLORS } from '../constants';

export default function ListingCard({
  listing,
  currentStudent,
  onViewDetails,
  onEdit,
  onCancel,
}) {
  const isOwner = listing.sellerId === currentStudent.id;
  const isCancelled = listing.status === 'CANCELLED';
  const isEnded = listing.status === 'ENDED' || listing.expired;

  // Countdown timer logic
  const [timeLeft, setTimeLeft] = useState(() => formatTimeRemaining(listing.closingTime));

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft(formatTimeRemaining(listing.closingTime));
    }, 1000);
    return () => clearInterval(timer);
  }, [listing.closingTime]);

  const photos = listing.photoUrls && listing.photoUrls.length > 0
    ? listing.photoUrls
    : ['https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80'];

  const coverPhoto = photos[0];
  const conditionMeta = CONDITION_COLORS[listing.condition] || { label: listing.condition, class: 'badge-blue' };
  const categoryIcon = CATEGORY_ICONS[listing.category] || '📦';

  return (
    <article className={`listing-card ${isCancelled ? 'card-cancelled' : ''}`}>
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
        ) : isEnded ? (
          <span className="status-pill status-ended">⌛ Auction Ended</span>
        ) : (
          <span className="status-pill status-active">
            ⏳ {timeLeft}
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

        {/* Pricing Specs */}
        <div className="pricing-grid">
          <div className="price-item">
            <span className="price-label">Starting Price</span>
            <span className="price-value">${Number(listing.startingPrice).toFixed(2)}</span>
          </div>
          <div className="price-item">
            <span className="price-label">Min Increment</span>
            <span className="price-increment">+${Number(listing.minBidIncrement).toFixed(2)}</span>
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
            title="View item photos and details"
          >
            Details
          </button>

          {isOwner ? (
            <>
              <button
                className="btn-edit btn-sm"
                onClick={() => onEdit(listing)}
                disabled={isCancelled}
                title={isCancelled ? 'Cancelled listings cannot be edited' : 'Edit your listing'}
              >
                ✏️ Edit
              </button>
              <button
                className="btn-cancel btn-sm"
                onClick={() => onCancel(listing)}
                disabled={isCancelled}
                title={isCancelled ? 'Listing is already cancelled' : 'Cancel this listing'}
              >
                {isCancelled ? 'Cancelled' : '🛑 Cancel'}
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

function formatTimeRemaining(closingTimeStr) {
  if (!closingTimeStr) return 'Ended';
  const closing = new Date(closingTimeStr);
  const now = new Date();
  const diffMs = closing - now;
  if (diffMs <= 0) return 'Ended';

  const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
  const seconds = Math.floor((diffMs % (1000 * 60)) / 1000);

  if (days > 0) return `${days}d ${hours}h left`;
  if (hours > 0) return `${hours}h ${minutes}m left`;
  return `${minutes}m ${seconds}s left`;
}

function formatDate(dateStr) {
  if (!dateStr) return 'N/A';
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateStr;
  }
}
