import { useState, useEffect } from 'react';
import { CONDITION_COLORS, CATEGORY_ICONS, getStatusBadge, calculateTimeRemaining, formatFullDate } from '../constants';
import { listingApi } from '../api/listingApi';

export default function ListingDetailModal({
  isOpen,
  onClose,
  listing,
  currentStudent,
  onEdit,
  onCancel,
  onBidPlaced,
}) {
  if (!isOpen || !listing) return null;

  return (
    <ListingDetailView
      key={listing.id}
      initialListing={listing}
      onClose={onClose}
      currentStudent={currentStudent}
      onEdit={onEdit}
      onCancel={onCancel}
      onBidPlaced={onBidPlaced}
    />
  );
}

function ListingDetailView({
  initialListing,
  onClose,
  currentStudent,
  onEdit,
  onCancel,
  onBidPlaced,
}) {
  const [listing, setListing] = useState(initialListing);
  const [prevInitial, setPrevInitial] = useState(initialListing);
  if (initialListing !== prevInitial) {
    setPrevInitial(initialListing);
    setListing(initialListing);
  }

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

  const isOwner = listing.sellerId === currentStudent?.id;
  const isCancelled = listing.status === 'CANCELLED';
  const isClosed = listing.status === 'ENDED' ||
    listing.status === 'CLOSED' ||
    listing.status === 'AWAITING_SELLER_APPROVAL' ||
    listing.status === 'APPROVED' ||
    listing.status === 'REJECTED' ||
    listing.status === 'UNSOLD' ||
    listing.status === 'COMPLETED' ||
    listing.expired ||
    countdown.isExpired;

  const conditionMeta = CONDITION_COLORS[listing.condition] || { label: listing.condition, class: 'badge-blue' };

  // Calculate bids, current leading bid, and minimum permitted next bid
  const bids = listing.bidHistory && listing.bidHistory.length > 0
    ? [...listing.bidHistory].sort((a, b) => Number(b.amount) - Number(a.amount))
    : [];
  const hasBids = bids.length > 0;

  const leadingBid = listing.currentLeadingBid != null
    ? Number(listing.currentLeadingBid)
    : (listing.currentPrice != null ? Number(listing.currentPrice) : Number(listing.startingPrice));

  const minNextBid = listing.minNextBid != null
    ? Number(listing.minNextBid)
    : (hasBids
        ? leadingBid + Number(listing.minBidIncrement || 1)
        : Number(listing.startingPrice));

  const statusBadge = getStatusBadge(listing.status, isClosed, hasBids);
  const isWinner = listing.winningBidderId === currentStudent?.id;

  // Bidding form state
  const [customBid, setCustomBid] = useState('');
  const bidAmount = customBid !== '' ? customBid : minNextBid.toFixed(2);
  const [submittingBid, setSubmittingBid] = useState(false);
  const [bidError, setBidError] = useState('');
  const [bidSuccess, setBidSuccess] = useState('');

  // Seller Decision & Sale Handover state
  const [sellerNotes, setSellerNotes] = useState('');
  const [submittingDecision, setSubmittingDecision] = useState(false);
  const [confirmingHandover, setConfirmingHandover] = useState(false);
  const [cancellingSale, setCancellingSale] = useState(false);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  const handleBidSubmit = async (e) => {
    if (e) e.preventDefault();
    setBidError('');
    setBidSuccess('');

    const numericAmount = parseFloat(bidAmount);
    if (isNaN(numericAmount) || numericAmount <= 0) {
      setBidError('Please enter a valid positive bid amount.');
      return;
    }

    if (numericAmount < minNextBid) {
      setBidError(
        `Your bid ($${numericAmount.toFixed(2)}) is below the required minimum of $${minNextBid.toFixed(2)}.`
      );
      return;
    }

    setSubmittingBid(true);
    try {
      const updated = await listingApi.placeBid(
        listing.id,
        {
          amount: numericAmount,
          bidderId: currentStudent.id,
          bidderName: currentStudent.name,
        },
        currentStudent.id
      );

      setListing(updated);
      setCustomBid('');
      setBidSuccess(
        `🎉 Bid of $${numericAmount.toFixed(2)} accepted! You are now the leading bidder on LOT #${String(listing.id).padStart(3, '0')}.`
      );

      if (onBidPlaced) {
        onBidPlaced(updated);
      }
    } catch (err) {
      setBidError(err.message || 'Failed to place bid. Another student may have placed a higher bid.');
      try {
        const fresh = await listingApi.getListingById(listing.id);
        setListing(fresh);
      } catch {
        // ignore
      }
    } finally {
      setSubmittingBid(false);
    }
  };

  const handleSellerDecision = async (decision) => {
    setActionError('');
    setActionSuccess('');
    setSubmittingDecision(true);
    try {
      const updated = await listingApi.submitDecision(
        listing.id,
        decision,
        sellerNotes,
        currentStudent.id
      );
      setListing(updated);
      setSellerNotes('');
      if (decision === 'APPROVE') {
        setActionSuccess(`🎉 Winning bid approved! Item LOT #${String(listing.id).padStart(3, '0')} is now reserved for ${updated.winningBidderName || updated.winningBidderId}. Coordinate campus pickup below.`);
      } else {
        setActionSuccess(`Winning bid was rejected. The auction is closed as unsold.`);
      }
      if (onBidPlaced) onBidPlaced(updated);
    } catch (err) {
      setActionError(err.message || 'Failed to submit decision.');
    } finally {
      setSubmittingDecision(false);
    }
  };

  const handleConfirmHandover = async () => {
    setActionError('');
    setActionSuccess('');
    setConfirmingHandover(true);
    try {
      const updated = await listingApi.confirmHandover(listing.id, currentStudent.id);
      setListing(updated);
      if (updated.status === 'COMPLETED') {
        setActionSuccess('🎉 Campus handover confirmed by both parties! Sale is now marked COMPLETED.');
      } else {
        setActionSuccess('✅ Your handover confirmation has been recorded! Waiting for the other student party to confirm.');
      }
      if (onBidPlaced) onBidPlaced(updated);
    } catch (err) {
      setActionError(err.message || 'Failed to confirm handover.');
    } finally {
      setConfirmingHandover(false);
    }
  };

  const handleCancelSale = async () => {
    const reason = window.prompt(
      'Enter reason for cancelling sale / failed pickup (e.g., "Buyer did not show up for scheduled pickup"):',
      'Buyer did not show up for scheduled pickup'
    );
    if (!reason || !reason.trim()) return;

    setActionError('');
    setActionSuccess('');
    setCancellingSale(true);
    try {
      const updated = await listingApi.cancelSale(listing.id, reason.trim(), currentStudent.id);
      setListing(updated);
      setActionSuccess('Sale cancelled. Listing and bidding history have been preserved in MySQL.');
      if (onBidPlaced) onBidPlaced(updated);
    } catch (err) {
      setActionError(err.message || 'Failed to cancel sale.');
    } finally {
      setCancellingSale(false);
    }
  };

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
              <span className={`status-pill ${statusBadge.class}`}>{statusBadge.label}</span>
            ) : (
              <span className="status-pill status-active">⏳ {countdown.text}</span>
            )}
          </div>
          <button type="button" className="modal-close-btn" onClick={onClose}>✕</button>
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

          {/* Right Column: Listing Details & Lifecycle Action Hub */}
          <div className="detail-content">
            <h1 className="detail-title">{listing.title}</h1>

            {/* Auction Pricing & Leading Bid Box */}
            <div className="detail-price-box">
              <div className="price-item detail-leading-bid-box">
                <span className="price-label">
                  {isClosed
                    ? (listing.winningBidAmount ? 'Winning Bid' : (hasBids ? 'Final Leading Bid' : 'Starting Price'))
                    : (hasBids ? 'Current Leading Bid' : 'Starting Price')}
                </span>
                <span className="price-large">
                  ${Number(listing.winningBidAmount || leadingBid).toFixed(2)}
                </span>
                {hasBids ? (
                  <span className="bid-status-note">
                    🔥 {isClosed ? 'Final' : 'Leading'} bid ({bids.length} {bids.length === 1 ? 'bid' : 'bids'})
                  </span>
                ) : (
                  <span className="bid-status-note">No bids placed</span>
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

            {/* Auction Timing & Lifecycle Info */}
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
                <span className="timing-label">Sale Lifecycle Status:</span>
                <span className="timing-val">
                  <strong>{statusBadge.label}</strong>
                </span>
              </div>
            </div>

            {/* Lifecycle Action Hub */}
            <div className="bidding-action-hub">
              {isCancelled ? (
                <div className="auction-unavailable-notice notice-cancelled">
                  <span className="prohibition-icon">🚫</span>
                  <div>
                    <strong>Listing Cancelled</strong>
                    <p>This auction was withdrawn by the seller and is retained in the database for record-keeping only.</p>
                  </div>
                </div>
              ) : listing.status === 'COMPLETED' ? (
                <div className="sale-completed-hub">
                  <div className="sale-completed-banner">
                    <span className="celebration-icon">🎉</span>
                    <div className="completed-meta">
                      <h3 className="completed-title">Sale Successfully Completed</h3>
                      <p className="completed-desc">
                        Physical campus handover has been confirmed by both parties.
                      </p>
                    </div>
                  </div>
                  <div className="completed-summary-grid">
                    <div className="summary-item">
                      <span className="summary-label">Winning Buyer</span>
                      <span className="summary-value">
                        👤 {listing.winningBidderName || listing.winningBidderId} {isWinner && <span className="you-bidder-badge">You</span>}
                      </span>
                    </div>
                    <div className="summary-item">
                      <span className="summary-label">Final Sale Price</span>
                      <span className="summary-value price-text">${Number(listing.winningBidAmount || leadingBid).toFixed(2)}</span>
                    </div>
                    <div className="summary-item">
                      <span className="summary-label">Handover Confirmed</span>
                      <span className="summary-value">{listing.handoverTime ? formatFullDate(listing.handoverTime) : 'Completed'}</span>
                    </div>
                  </div>
                  <div className="handover-badges-row">
                    <span className="handover-badge-pill done">✓ Seller Handover Confirmed</span>
                    <span className="handover-badge-pill done">✓ Buyer Handover Confirmed</span>
                  </div>
                </div>
              ) : listing.status === 'APPROVED' ? (
                <div className="sale-handover-hub">
                  <div className="reservation-banner">
                    <span className="res-icon">✅</span>
                    <div>
                      <h3 className="res-title">Winning Bid Approved & Item Reserved</h3>
                      <p className="res-sub">
                        Reserved for <strong>{listing.winningBidderName || listing.winningBidderId}</strong> {isWinner && <span className="you-bidder-badge">You</span>} at <strong>${Number(listing.winningBidAmount || leadingBid).toFixed(2)}</strong>.
                      </p>
                      {listing.sellerDecisionNotes && (
                        <p className="seller-notes-quote">
                          💬 <em>Seller Note: &ldquo;{listing.sellerDecisionNotes}&rdquo;</em>
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="handover-tracker-card">
                    <div className="tracker-header">
                      <h4 className="tracker-title">🤝 Campus Handover Status</h4>
                      <span className="tracker-badge">Mutual Confirmation</span>
                    </div>
                    <p className="tracker-instruction">
                      Both the seller and winning buyer must confirm the physical exchange on campus to finalize the sale.
                    </p>

                    <div className="handover-parties-grid">
                      <div className={`party-status-box ${listing.handoverConfirmedBySeller ? 'confirmed' : 'pending'}`}>
                        <span className="party-role">Seller Handover</span>
                        <span className="party-name">{listing.sellerName || listing.sellerId}</span>
                        <span className="party-status-indicator">
                          {listing.handoverConfirmedBySeller ? '✅ Confirmed' : '⏳ Awaiting Confirmation'}
                        </span>
                      </div>

                      <div className={`party-status-box ${listing.handoverConfirmedByWinner ? 'confirmed' : 'pending'}`}>
                        <span className="party-role">Winning Buyer Handover</span>
                        <span className="party-name">{listing.winningBidderName || listing.winningBidderId}</span>
                        <span className="party-status-indicator">
                          {listing.handoverConfirmedByWinner ? '✅ Confirmed' : '⏳ Awaiting Confirmation'}
                        </span>
                      </div>
                    </div>

                    <div className="handover-actions-container">
                      {isOwner && !listing.handoverConfirmedBySeller && (
                        <button
                          type="button"
                          className="btn-primary btn-confirm-handover"
                          onClick={handleConfirmHandover}
                          disabled={confirmingHandover}
                        >
                          {confirmingHandover ? 'Confirming...' : '🤝 Confirm Campus Handover (Seller)'}
                        </button>
                      )}

                      {isWinner && !listing.handoverConfirmedByWinner && (
                        <button
                          type="button"
                          className="btn-primary btn-confirm-handover"
                          onClick={handleConfirmHandover}
                          disabled={confirmingHandover}
                        >
                          {confirmingHandover ? 'Confirming...' : '🤝 Confirm Item Received (Buyer)'}
                        </button>
                      )}

                      {isOwner && (
                        <button
                          type="button"
                          className="btn-cancel-sale"
                          onClick={handleCancelSale}
                          disabled={cancellingSale}
                          title="Cancel sale if buyer did not arrive for scheduled pickup"
                        >
                          {cancellingSale ? 'Cancelling...' : '🛑 Report Failed Pickup / Cancel Sale'}
                        </button>
                      )}
                    </div>

                    {actionError && (
                      <div className="bid-alert-banner alert-error" style={{ marginTop: '0.75rem' }}>
                        <span className="alert-icon">⚠️</span>
                        <span>{actionError}</span>
                      </div>
                    )}
                    {actionSuccess && (
                      <div className="bid-alert-banner alert-success" style={{ marginTop: '0.75rem' }}>
                        <span className="alert-icon">🎉</span>
                        <span>{actionSuccess}</span>
                      </div>
                    )}
                  </div>
                </div>
              ) : listing.status === 'REJECTED' ? (
                <div className="auction-unavailable-notice notice-rejected">
                  <span className="prohibition-icon">❌</span>
                  <div>
                    <strong>Winning Bid Rejected by Seller</strong>
                    <p>
                      The seller chose not to proceed with the winning bid of <strong>${Number(listing.winningBidAmount || leadingBid).toFixed(2)}</strong>. This auction is closed without sale.
                    </p>
                    {listing.sellerDecisionNotes && (
                      <p className="seller-notes-quote" style={{ marginTop: '0.5rem' }}>
                        💬 <em>Seller Reason: &ldquo;{listing.sellerDecisionNotes}&rdquo;</em>
                      </p>
                    )}
                  </div>
                </div>
              ) : listing.status === 'UNSOLD' || (isClosed && !hasBids) ? (
                <div className="auction-unavailable-notice notice-unsold">
                  <span className="prohibition-icon">🚫</span>
                  <div>
                    <strong>Auction Ended Without Bids (Unsold)</strong>
                    <p>This auction reached its closing deadline without receiving any bids.</p>
                  </div>
                </div>
              ) : listing.status === 'AWAITING_SELLER_APPROVAL' || (isClosed && hasBids) ? (
                <div className="seller-decision-hub">
                  {isOwner ? (
                    <div className="decision-station-card">
                      <div className="station-header">
                        <span className="station-badge">⚡ Action Required</span>
                        <h3 className="station-title">Review Winning Bid & Decide</h3>
                      </div>
                      <p className="station-intro">
                        This auction is closed. As the seller, review the highest bid below and either <strong>APPROVE</strong> to reserve the item for campus handover, or <strong>REJECT</strong>.
                      </p>

                      <div className="decision-winner-spotlight">
                        <div className="spotlight-metric">
                          <span className="metric-label">Winning Bid</span>
                          <span className="metric-value price-green">${Number(listing.winningBidAmount || leadingBid).toFixed(2)}</span>
                        </div>
                        <div className="spotlight-metric">
                          <span className="metric-label">Winning Bidder</span>
                          <span className="metric-value">
                            👤 {listing.winningBidderName || listing.winningBidderId} <code>({listing.winningBidderId})</code>
                          </span>
                        </div>
                        <div className="spotlight-metric">
                          <span className="metric-label">Total Bids</span>
                          <span className="metric-value">{bids.length} bids</span>
                        </div>
                      </div>

                      <div className="decision-notes-group">
                        <label className="decision-notes-label" htmlFor="seller-notes-input">
                          Decision Notes or Handover Instructions (Optional):
                        </label>
                        <textarea
                          id="seller-notes-input"
                          className="decision-notes-textarea"
                          rows={2}
                          placeholder="e.g. 'Approved! Meet tomorrow 2 PM at campus student center.' or reason if rejecting."
                          value={sellerNotes}
                          onChange={(e) => setSellerNotes(e.target.value)}
                          disabled={submittingDecision}
                        />
                      </div>

                      <div className="decision-action-buttons">
                        <button
                          type="button"
                          className="btn-approve-bid"
                          onClick={() => handleSellerDecision('APPROVE')}
                          disabled={submittingDecision}
                        >
                          {submittingDecision ? 'Processing...' : '✅ Approve Winning Bid'}
                        </button>
                        <button
                          type="button"
                          className="btn-reject-bid"
                          onClick={() => handleSellerDecision('REJECT')}
                          disabled={submittingDecision}
                        >
                          {submittingDecision ? 'Processing...' : '❌ Reject Winning Bid'}
                        </button>
                      </div>

                      {actionError && (
                        <div className="bid-alert-banner alert-error" style={{ marginTop: '0.75rem' }}>
                          <span className="alert-icon">⚠️</span>
                          <span>{actionError}</span>
                        </div>
                      )}
                      {actionSuccess && (
                        <div className="bid-alert-banner alert-success" style={{ marginTop: '0.75rem' }}>
                          <span className="alert-icon">🎉</span>
                          <span>{actionSuccess}</span>
                        </div>
                      )}
                    </div>
                  ) : isWinner ? (
                    <div className="winner-pending-banner">
                      <span className="winner-banner-icon">🏆</span>
                      <div className="winner-banner-content">
                        <h3 className="winner-banner-title">You Placed the Winning Bid!</h3>
                        <p className="winner-banner-desc">
                          Your bid of <strong>${Number(listing.winningBidAmount || leadingBid).toFixed(2)}</strong> is the highest valid bid. The auction has closed and is currently <strong>Awaiting Seller Approval</strong>.
                        </p>
                        <p className="winner-banner-sub">
                          Once the seller reviews and approves your bid, this lot will be reserved for you to coordinate campus pickup.
                        </p>
                      </div>
                    </div>
                  ) : (
                    <div className="auction-unavailable-notice notice-awaiting">
                      <span className="prohibition-icon">⌛</span>
                      <div>
                        <strong>Auction Closed — Awaiting Seller Decision</strong>
                        <p>
                          Auction ended with a winning bid of <strong>${Number(listing.winningBidAmount || leadingBid).toFixed(2)}</strong> placed by 👤 <strong>{listing.winningBidderName || listing.winningBidderId}</strong>.
                        </p>
                        <p>Awaiting seller approval.</p>
                      </div>
                    </div>
                  )}
                </div>
              ) : isOwner ? (
                <div className="seller-bid-prohibition">
                  <span className="prohibition-icon">🔒</span>
                  <div>
                    <strong>Seller Bidding Prohibited</strong>
                    <p>You listed this item under Student ID <code>{listing.sellerId}</code>. In accordance with CampusBid fair auction rules, sellers cannot place bids on their own lots.</p>
                  </div>
                </div>
              ) : (
                /* Live Active Bidding Form */
                <form className="bid-submission-form" onSubmit={handleBidSubmit}>
                  <div className="bidding-hub-header">
                    <div className="hub-title-group">
                      <span className="hub-badge">🔨 Live Campus Auction</span>
                      <h3 className="hub-title">Place Your Bid</h3>
                    </div>
                    <div className="current-bidder-indicator" title={`Active Bidder: ${currentStudent.name} (${currentStudent.id})`}>
                      <span className="bidder-avatar">{currentStudent.avatar || '🎓'}</span>
                      <div className="bidder-meta">
                        <span className="bidder-label">Bidding As</span>
                        <span className="bidder-name">{currentStudent.name} <code>({currentStudent.id})</code></span>
                      </div>
                    </div>
                  </div>

                  <div className="bidding-specs-row">
                    <div className="bidding-spec-card leading-spec">
                      <span className="spec-label">{hasBids ? 'Current Leading Bid' : 'Opening Price'}</span>
                      <span className="spec-amount">${leadingBid.toFixed(2)}</span>
                      <span className="spec-sub">
                        {hasBids ? `🔥 ${bids.length} ${bids.length === 1 ? 'bid' : 'bids'} placed` : 'Opens at starting price'}
                      </span>
                    </div>
                    <div className="bidding-spec-card minimum-spec">
                      <span className="spec-label">Minimum Required Next Bid</span>
                      <span className="spec-amount text-orange">${minNextBid.toFixed(2)}</span>
                      <span className="spec-sub">
                        {hasBids ? `+ $${Number(listing.minBidIncrement).toFixed(2)} minimum step` : 'First bid must meet start price'}
                      </span>
                    </div>
                  </div>

                  {/* Quick Bid Preset Buttons */}
                  <div className="quick-bids-row">
                    <span className="quick-bids-label">Quick Bid Presets:</span>
                    <button
                      type="button"
                      className="btn-quick-bid"
                      onClick={() => setCustomBid(minNextBid.toFixed(2))}
                      disabled={submittingBid}
                    >
                      Min: ${minNextBid.toFixed(2)}
                    </button>
                    <button
                      type="button"
                      className="btn-quick-bid"
                      onClick={() => setCustomBid((minNextBid + Number(listing.minBidIncrement || 1)).toFixed(2))}
                      disabled={submittingBid}
                    >
                      +${Number(listing.minBidIncrement || 1).toFixed(2)} (${(minNextBid + Number(listing.minBidIncrement || 1)).toFixed(2)})
                    </button>
                    <button
                      type="button"
                      className="btn-quick-bid"
                      onClick={() => setCustomBid((minNextBid + Number(listing.minBidIncrement || 1) * 2).toFixed(2))}
                      disabled={submittingBid}
                    >
                      +${(Number(listing.minBidIncrement || 1) * 2).toFixed(2)} (${(minNextBid + Number(listing.minBidIncrement || 1) * 2).toFixed(2)})
                    </button>
                  </div>

                  {/* Custom Bid Input & Place Bid Button */}
                  <div className="bid-input-action-row">
                    <div className="bid-input-wrapper">
                      <span className="currency-prefix">$</span>
                      <input
                        type="number"
                        step="0.01"
                        min={minNextBid}
                        className="bid-custom-input"
                        placeholder={minNextBid.toFixed(2)}
                        value={bidAmount}
                        onChange={(e) => setCustomBid(e.target.value)}
                        disabled={submittingBid}
                        required
                      />
                    </div>

                    <button
                      type="submit"
                      className="btn-primary btn-place-bid"
                      disabled={submittingBid || parseFloat(bidAmount || 0) < minNextBid}
                    >
                      {submittingBid ? (
                        <span>Submitting Bid...</span>
                      ) : (
                        <span>🔨 Place Bid of ${parseFloat(bidAmount || 0).toFixed(2)}</span>
                      )}
                    </button>
                  </div>

                  {/* Inline Error & Success Feedback */}
                  {bidError && (
                    <div className="bid-alert-banner alert-error">
                      <span className="alert-icon">⚠️</span>
                      <span>{bidError}</span>
                    </div>
                  )}

                  {bidSuccess && (
                    <div className="bid-alert-banner alert-success">
                      <span className="alert-icon">🎉</span>
                      <span>{bidSuccess}</span>
                    </div>
                  )}
                </form>
              )}
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
                              <span className="leading-rank-tag">{isClosed ? '🏆 Winner' : '🏆 Leading'}</span>
                            ) : (
                              <span className="rank-tag">#{idx + 1}</span>
                            )}
                          </td>
                          <td className="bidder-cell">
                            <span className="bidder-icon">👤</span>
                            <span className="bidder-name">{bid.bidderName || bid.bidderId}</span>
                            {bid.bidderId === currentStudent?.id && (
                              <span className="you-bidder-badge">You</span>
                            )}
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
                      Bidding starts at <strong>${Number(listing.startingPrice).toFixed(2)}</strong> with minimum increments of <strong>+${Number(listing.minBidIncrement).toFixed(2)}</strong>.
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
                    type="button"
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
                    type="button"
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

