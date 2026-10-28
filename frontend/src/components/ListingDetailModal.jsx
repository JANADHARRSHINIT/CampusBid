import { useState } from 'react';
import { CONDITION_COLORS, CATEGORY_ICONS } from '../constants';

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

  const photos = listing.photoUrls && listing.photoUrls.length > 0
    ? listing.photoUrls
    : ['https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80'];

  const isOwner = listing.sellerId === currentStudent.id;
  const isCancelled = listing.status === 'CANCELLED';
  const isEnded = listing.status === 'ENDED' || listing.expired;
  const conditionMeta = CONDITION_COLORS[listing.condition] || { label: listing.condition, class: 'badge-blue' };

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
            {isCancelled && <span className="status-pill status-cancelled">🚫 Cancelled</span>}
            {isEnded && !isCancelled && <span className="status-pill status-ended">⌛ Ended</span>}
            {!isCancelled && !isEnded && <span className="status-pill status-active">⏳ Live Auction</span>}
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

            <div className="detail-price-box">
              <div className="price-item">
                <span className="price-label">Starting Price</span>
                <span className="price-large">${Number(listing.startingPrice).toFixed(2)}</span>
              </div>
              <div className="price-item">
                <span className="price-label">Min Bid Increment</span>
                <span className="price-increment-large">+${Number(listing.minBidIncrement).toFixed(2)}</span>
              </div>
            </div>

            {/* Auction Timing */}
            <div className="detail-timing-box">
              <div className="timing-row">
                <span className="timing-label">Closing Date & Time:</span>
                <span className="timing-val">{formatFullDate(listing.closingTime)}</span>
              </div>
              <div className="timing-row">
                <span className="timing-label">Status:</span>
                <span className="timing-val">
                  {isCancelled ? 'Cancelled (Kept in Database)' : isEnded ? 'Auction Ended' : 'Open for Bidding'}
                </span>
              </div>
            </div>

            {/* Description */}
            <div className="detail-section">
              <h3 className="section-heading">Description</h3>
              <p className="detail-description-text">{listing.description}</p>
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
                    disabled={isCancelled}
                    title={isCancelled ? 'Cancelled listings cannot be edited' : 'Edit listing'}
                  >
                    ✏️ Edit Listing
                  </button>
                  <button
                    className="btn-cancel"
                    onClick={() => {
                      onClose();
                      onCancel(listing);
                    }}
                    disabled={isCancelled}
                    title={isCancelled ? 'Already cancelled' : 'Cancel listing'}
                  >
                    {isCancelled ? 'Already Cancelled' : '🛑 Cancel Listing'}
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

function formatFullDate(dateStr) {
  if (!dateStr) return 'N/A';
  try {
    const d = new Date(dateStr);
    return d.toLocaleString(undefined, {
      weekday: 'short',
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
