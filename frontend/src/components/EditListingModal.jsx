import { useState } from 'react';
import { listingApi } from '../api/listingApi';
import { CATEGORIES, CONDITIONS } from '../constants';

const FORM_CATEGORIES = CATEGORIES.filter(c => c !== 'All');

function formatDateTimeLocal(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

function getMinClosingTime() {
  const d = new Date();
  d.setMinutes(d.getMinutes() + 5);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

export default function EditListingModal({
  isOpen,
  onClose,
  listing,
  currentStudent,
  onSuccess,
  showToast,
}) {
  if (!isOpen || !listing) return null;

  return (
    <EditListingForm
      key={listing.id}
      listing={listing}
      onClose={onClose}
      currentStudent={currentStudent}
      onSuccess={onSuccess}
      showToast={showToast}
    />
  );
}

function EditListingForm({
  listing,
  onClose,
  currentStudent,
  onSuccess,
  showToast,
}) {
  const [formData, setFormData] = useState({
    title: listing.title || '',
    category: listing.category || 'Textbooks',
    description: listing.description || '',
    condition: listing.condition || 'LIKE_NEW',
    minBidIncrement: listing.minBidIncrement ? String(listing.minBidIncrement) : '5.00',
    closingTime: formatDateTimeLocal(listing.closingTime),
  });

  const [previewUrls, setPreviewUrls] = useState(listing.photoUrls ? [...listing.photoUrls] : []);
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [urlInput, setUrlInput] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleFileChange(e) {
    const files = Array.from(e.target.files);
    if (!files.length) return;

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    const maxFileSize = 5 * 1024 * 1024;

    const validFiles = [];
    const newErrors = { ...fieldErrors };

    if (previewUrls.length + files.length > 5) {
      newErrors.photos = 'Maximum 5 photos allowed per listing.';
      setFieldErrors(newErrors);
      return;
    }

    for (const file of files) {
      if (!allowedTypes.includes(file.type)) {
        newErrors.photos = `Unsupported format for "${file.name}". Please upload JPEG, PNG, or WEBP.`;
        setFieldErrors(newErrors);
        return;
      }
      if (file.size > maxFileSize) {
        newErrors.photos = `File "${file.name}" exceeds 5MB size limit.`;
        setFieldErrors(newErrors);
        return;
      }
      validFiles.push(file);
    }

    delete newErrors.photos;
    setFieldErrors(newErrors);

    const newPreviews = validFiles.map((f) => URL.createObjectURL(f));
    setUploadedFiles((prev) => [...prev, ...validFiles]);
    setPreviewUrls((prev) => [...prev, ...newPreviews]);
  }

  function handleAddUrlPhoto() {
    if (!urlInput.trim()) return;
    if (previewUrls.length >= 5) {
      setFieldErrors((prev) => ({ ...prev, photos: 'Maximum 5 photos allowed.' }));
      return;
    }
    setPreviewUrls((prev) => [...prev, urlInput.trim()]);
    setUrlInput('');
  }

  function handleRemovePhoto(index) {
    setPreviewUrls((prev) => prev.filter((_, i) => i !== index));
    setUploadedFiles((prev) => prev.filter((_, i) => i !== index));
  }

  function validate() {
    const errors = {};
    if (!formData.title.trim() || formData.title.trim().length < 3) {
      errors.title = 'Title must be between 3 and 150 characters.';
    }
    if (!formData.description.trim() || formData.description.trim().length < 10) {
      errors.description = 'Description must be at least 10 characters long.';
    }
    const increment = parseFloat(formData.minBidIncrement);
    if (isNaN(increment) || increment <= 0) {
      errors.minBidIncrement = 'Minimum increment must be positive.';
    }
    if (!formData.closingTime) {
      errors.closingTime = 'Closing date and time is required.';
    } else {
      const closingDate = new Date(formData.closingTime);
      if (closingDate <= new Date()) {
        errors.closingTime = 'Closing time must be set in the future.';
      }
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!validate()) {
      showToast('Please fix highlighted errors.', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      let finalPhotoUrls = [];

      if (uploadedFiles.length > 0) {
        showToast('Uploading new images...', 'info');
        const uploadedPaths = await listingApi.uploadImages(uploadedFiles);
        finalPhotoUrls = [...uploadedPaths];
      }

      previewUrls.forEach((url) => {
        if (!url.startsWith('blob:') && !finalPhotoUrls.includes(url)) {
          finalPhotoUrls.push(url);
        }
      });

      if (finalPhotoUrls.length === 0) {
        finalPhotoUrls.push('https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80');
      }

      const payload = {
        title: formData.title.trim(),
        category: formData.category,
        description: formData.description.trim(),
        condition: formData.condition,
        minBidIncrement: parseFloat(formData.minBidIncrement),
        closingTime: new Date(formData.closingTime).toISOString().slice(0, 19),
        photoUrls: finalPhotoUrls,
      };

      const updated = await listingApi.updateListing(listing.id, payload, currentStudent.id);
      showToast(`✏️ Listing "${updated.title}" updated successfully!`, 'success');
      onSuccess(updated);
      onClose();
    } catch (err) {
      if (err.fieldErrors) {
        setFieldErrors(err.fieldErrors);
      }
      showToast(err.message || 'Error updating listing.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-container modal-lg" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h2 className="modal-title">Edit Listing #{listing.id}</h2>
            <p className="modal-subtitle">
              Seller: <strong>{listing.sellerName || listing.sellerId}</strong>
            </p>
          </div>
          <button className="modal-close-btn" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          <div className="form-grid">
            <div className="form-group span-full">
              <label htmlFor="edit-title" className="form-label">Item Title *</label>
              <input
                id="edit-title"
                type="text"
                className={`form-input ${fieldErrors.title ? 'input-error' : ''}`}
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                maxLength={150}
              />
              {fieldErrors.title && <span className="error-hint">{fieldErrors.title}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="edit-category" className="form-label">Category *</label>
              <select
                id="edit-category"
                className="form-select"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              >
                {FORM_CATEGORIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="edit-condition" className="form-label">Condition *</label>
              <select
                id="edit-condition"
                className="form-select"
                value={formData.condition}
                onChange={(e) => setFormData({ ...formData, condition: e.target.value })}
              >
                {CONDITIONS.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </div>

            <div className="form-group span-full">
              <label htmlFor="edit-description" className="form-label">Description *</label>
              <textarea
                id="edit-description"
                rows={4}
                className={`form-textarea ${fieldErrors.description ? 'input-error' : ''}`}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
              {fieldErrors.description && <span className="error-hint">{fieldErrors.description}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="edit-minBidIncrement" className="form-label">Min Bid Increment ($ USD) *</label>
              <div className="input-prefix-wrapper">
                <span className="input-prefix">$</span>
                <input
                  id="edit-minBidIncrement"
                  type="number"
                  step="0.01"
                  min="0.01"
                  className={`form-input has-prefix ${fieldErrors.minBidIncrement ? 'input-error' : ''}`}
                  value={formData.minBidIncrement}
                  onChange={(e) => setFormData({ ...formData, minBidIncrement: e.target.value })}
                />
              </div>
              {fieldErrors.minBidIncrement && <span className="error-hint">{fieldErrors.minBidIncrement}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="edit-closingTime" className="form-label">Closing Date & Time *</label>
              <input
                id="edit-closingTime"
                type="datetime-local"
                min={getMinClosingTime()}
                className={`form-input ${fieldErrors.closingTime ? 'input-error' : ''}`}
                value={formData.closingTime}
                onChange={(e) => setFormData({ ...formData, closingTime: e.target.value })}
              />
              {fieldErrors.closingTime && <span className="error-hint">{fieldErrors.closingTime}</span>}
            </div>

            <div className="form-group span-full">
              <label className="form-label">Manage Photos (Max 5)</label>
              <div className="upload-dropzone">
                <input
                  type="file"
                  id="edit-photo-upload"
                  className="file-hidden-input"
                  multiple
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  onChange={handleFileChange}
                  disabled={previewUrls.length >= 5}
                />
                <label htmlFor="edit-photo-upload" className="dropzone-label">
                  <span className="upload-icon">📸</span>
                  <span className="upload-title">Upload more photos</span>
                  <span className="upload-sub">JPEG, PNG, WEBP (Max 5MB each)</span>
                </label>
              </div>

              <div className="url-photo-row">
                <input
                  type="url"
                  className="form-input"
                  placeholder="Or paste an image web link..."
                  value={urlInput}
                  onChange={(e) => setUrlInput(e.target.value)}
                  disabled={previewUrls.length >= 5}
                />
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={handleAddUrlPhoto}
                  disabled={!urlInput.trim() || previewUrls.length >= 5}
                >
                  + Add URL
                </button>
              </div>

              {previewUrls.length > 0 && (
                <div className="photo-preview-grid">
                  {previewUrls.map((url, idx) => (
                    <div key={idx} className="preview-thumb">
                      <img src={url} alt={`Photo ${idx + 1}`} />
                      <button
                        type="button"
                        className="thumb-remove-btn"
                        onClick={() => handleRemovePhoto(idx)}
                      >
                        ✕
                      </button>
                      {idx === 0 && <span className="cover-badge">Cover</span>}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={isSubmitting}>
              {isSubmitting ? (
                <>
                  <span className="spinner"></span> Saving Changes...
                </>
              ) : (
                'Save Changes'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
