import { useState } from 'react';
import { listingApi } from '../api/listingApi';

export default function CancelModal({
  isOpen,
  onClose,
  listing,
  currentStudent,
  onSuccess,
  showToast,
}) {
  const [isCancelling, setIsCancelling] = useState(false);

  if (!isOpen || !listing) return null;

  async function handleConfirmCancel() {
    setIsCancelling(true);
    try {
      const cancelled = await listingApi.cancelListing(listing.id, currentStudent.id);
      showToast(`🛑 Listing "${cancelled.title}" has been cancelled.`, 'info');
      onSuccess(cancelled);
      onClose();
    } catch (err) {
      showToast(err.message || 'Failed to cancel listing.', 'error');
    } finally {
      setIsCancelling(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-container modal-sm" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-warning-header">
            <span className="warning-icon">⚠️</span>
            <h2 className="modal-title">Confirm Listing Cancellation</h2>
          </div>
          <button className="modal-close-btn" onClick={onClose}>✕</button>
        </div>

        <div className="cancel-modal-body">
          <p className="cancel-prompt">
            Are you sure you want to cancel the auction listing for:
          </p>
          <div className="cancel-item-box">
            <span className="cancel-item-title">{listing.title}</span>
            <span className="cancel-item-meta">
              Listing ID: #{listing.id} • Starting: ${Number(listing.startingPrice).toFixed(2)}
            </span>
          </div>

          <div className="cancel-warning-note">
            <span className="note-icon">ℹ️</span>
            <div>
              <strong>Note:</strong> Withdrawing an auction cannot be undone. Bidding will immediately conclude, and the item will be marked as cancelled.
            </div>
          </div>

          <div className="seller-verify-tag">
            Listing Owner: <strong>{currentStudent.name}</strong> ({currentStudent.id})
          </div>
        </div>

        <div className="modal-footer">
          <button
            type="button"
            className="btn-secondary"
            onClick={onClose}
            disabled={isCancelling}
          >
            Keep Listing Active
          </button>
          <button
            type="button"
            className="btn-danger"
            onClick={handleConfirmCancel}
            disabled={isCancelling}
          >
            {isCancelling ? (
              <>
                <span className="spinner"></span> Cancelling...
              </>
            ) : (
              'Yes, Cancel Listing'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
