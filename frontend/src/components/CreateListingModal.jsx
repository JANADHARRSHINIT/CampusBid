import { useState } from 'react';
import { listingApi } from '../api/listingApi';
import { CATEGORIES, CONDITIONS } from '../constants';

const FORM_CATEGORIES = CATEGORIES.filter(c => c !== 'All');

export default function CreateListingModal({
  isOpen,
  onClose,
  currentStudent,
  onSuccess,
  showToast,
}) {
  const [formData, setFormData] = useState({
    title: '',
    category: 'Textbooks',
    description: '',
    condition: 'LIKE_NEW',
    startingPrice: '',
    minBidIncrement: '5.00',
    closingTime: getDefaultClosingTime(),
  });

  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [previewUrls, setPreviewUrls] = useState([]);
  const [urlInput, setUrlInput] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  function getDefaultClosingTime() {
    const d = new Date();
    d.setDate(d.getDate() + 3); // 3 days in the future
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
  }

  function getMinClosingTime() {
    const d = new Date();
    d.setMinutes(d.getMinutes() + 10); // At least 10 minutes in future
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
  }

  // Handle Photo File Selection
  function handleFileChange(e) {
    const files = Array.from(e.target.files);
    if (!files.length) return;

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    const maxFileSize = 5 * 1024 * 1024; // 5MB

    const validFiles = [];
    const newErrors = { ...fieldErrors };

    if (uploadedFiles.length + previewUrls.length + files.length > 5) {
      newErrors.photos = 'You can upload a maximum of 5 photos per listing.';
      setFieldErrors(newErrors);
      return;
    }

    for (const file of files) {
      if (!allowedTypes.includes(file.type)) {
        newErrors.photos = `File "${file.name}" is not a supported format. Please upload JPEG, PNG, or WEBP.`;
        setFieldErrors(newErrors);
        return;
      }
      if (file.size > maxFileSize) {
        newErrors.photos = `File "${file.name}" exceeds the 5MB size limit (${(file.size / (1024 * 1024)).toFixed(1)}MB).`;
        setFieldErrors(newErrors);
        return;
      }
      validFiles.push(file);
    }

    delete newErrors.photos;
    setFieldErrors(newErrors);

    // Create local object URLs for immediate preview
    const newPreviews = validFiles.map((f) => URL.createObjectURL(f));
    setUploadedFiles((prev) => [...prev, ...validFiles]);
    setPreviewUrls((prev) => [...prev, ...newPreviews]);
  }

  // Add photo via direct URL
  function handleAddUrlPhoto() {
    if (!urlInput.trim()) return;
    if (previewUrls.length >= 5) {
      setFieldErrors((prev) => ({ ...prev, photos: 'Maximum 5 photos allowed.' }));
      return;
    }
    setPreviewUrls((prev) => [...prev, urlInput.trim()]);
    setUrlInput('');
    setFieldErrors((prev) => {
      const copy = { ...prev };
      delete copy.photos;
      return copy;
    });
  }

  function handleRemovePhoto(index) {
    setPreviewUrls((prev) => prev.filter((_, i) => i !== index));
    setUploadedFiles((prev) => prev.filter((_, i) => i !== index));
  }

  // Client-side validation
  function validate() {
    const errors = {};
    if (!formData.title.trim() || formData.title.trim().length < 3) {
      errors.title = 'Title must be between 3 and 150 characters.';
    }
    if (!formData.category) {
      errors.category = 'Please select a category.';
    }
    if (!formData.description.trim() || formData.description.trim().length < 10) {
      errors.description = 'Description must be at least 10 characters long.';
    }
    if (!formData.condition) {
      errors.condition = 'Please select item condition.';
    }

    const price = parseFloat(formData.startingPrice);
    if (isNaN(price) || price <= 0) {
      errors.startingPrice = 'Starting price must be greater than $0.00.';
    }

    const increment = parseFloat(formData.minBidIncrement);
    if (isNaN(increment) || increment <= 0) {
      errors.minBidIncrement = 'Minimum increment must be greater than $0.00.';
    }

    if (!formData.closingTime) {
      errors.closingTime = 'Please select closing date and time.';
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
      showToast('Please fix the highlighted errors before submitting.', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      let finalPhotoUrls = [];

      // 1. Upload files to server if any local files were selected
      if (uploadedFiles.length > 0) {
        showToast('Uploading images...', 'info');
        const uploadedPaths = await listingApi.uploadImages(uploadedFiles);
        finalPhotoUrls = [...uploadedPaths];
      }

      // Add any direct external URLs that were not local files
      previewUrls.forEach((url) => {
        if (!url.startsWith('blob:') && !finalPhotoUrls.includes(url)) {
          finalPhotoUrls.push(url);
        }
      });

      // Default fallback photo if none provided
      if (finalPhotoUrls.length === 0) {
        finalPhotoUrls.push('https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80');
      }

      const payload = {
        title: formData.title.trim(),
        category: formData.category,
        description: formData.description.trim(),
        condition: formData.condition,
        startingPrice: parseFloat(formData.startingPrice),
        minBidIncrement: parseFloat(formData.minBidIncrement),
        closingTime: new Date(formData.closingTime).toISOString().slice(0, 19),
        photoUrls: finalPhotoUrls,
        sellerId: currentStudent.id,
        sellerName: currentStudent.name,
        sellerEmail: currentStudent.email,
      };

      const created = await listingApi.createListing(payload, currentStudent.id);
      showToast(`🎉 Listing "${created.title}" successfully created!`, 'success');
      onSuccess(created);
      onClose();
    } catch (err) {
      if (err.fieldErrors) {
        setFieldErrors(err.fieldErrors);
      }
      showToast(err.message || 'Error creating listing. Please check input.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-container modal-lg" onClick={(e) => e.stopPropagation()}>
        {/* Modal Header */}
        <div className="modal-header">
          <div>
            <h2 className="modal-title">Create Listing • Campus Marketplace</h2>
            <p className="modal-subtitle">
              Listing as: <strong>{currentStudent.name}</strong>
            </p>
          </div>
          <button className="modal-close-btn" onClick={onClose}>✕</button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="modal-form">
          <div className="form-grid">
            {/* Title */}
            <div className="form-group span-full">
              <label htmlFor="title" className="form-label">
                Item Title <span className="req">*</span>
              </label>
              <input
                id="title"
                type="text"
                className={`form-input ${fieldErrors.title ? 'input-error' : ''}`}
                placeholder="e.g. Calculus: Early Transcendentals 8th Edition"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                maxLength={150}
              />
              {fieldErrors.title && <span className="error-hint">{fieldErrors.title}</span>}
            </div>

            {/* Category */}
            <div className="form-group">
              <label htmlFor="category" className="form-label">
                Category <span className="req">*</span>
              </label>
              <select
                id="category"
                className="form-select"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              >
                {FORM_CATEGORIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            {/* Condition */}
            <div className="form-group">
              <label htmlFor="condition" className="form-label">
                Condition <span className="req">*</span>
              </label>
              <select
                id="condition"
                className="form-select"
                value={formData.condition}
                onChange={(e) => setFormData({ ...formData, condition: e.target.value })}
              >
                {CONDITIONS.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </div>

            {/* Description */}
            <div className="form-group span-full">
              <label htmlFor="description" className="form-label">
                Detailed Description <span className="req">*</span>
              </label>
              <textarea
                id="description"
                rows={4}
                className={`form-textarea ${fieldErrors.description ? 'input-error' : ''}`}
                placeholder="Describe item features, cosmetic condition, reason for selling, included accessories, campus pickup location..."
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
              {fieldErrors.description && <span className="error-hint">{fieldErrors.description}</span>}
            </div>

            {/* Starting Price */}
            <div className="form-group">
              <label htmlFor="startingPrice" className="form-label">
                Starting Price ($ USD) <span className="req">*</span>
              </label>
              <div className="input-prefix-wrapper">
                <span className="input-prefix">$</span>
                <input
                  id="startingPrice"
                  type="number"
                  step="0.01"
                  min="0.01"
                  className={`form-input has-prefix ${fieldErrors.startingPrice ? 'input-error' : ''}`}
                  placeholder="25.00"
                  value={formData.startingPrice}
                  onChange={(e) => setFormData({ ...formData, startingPrice: e.target.value })}
                />
              </div>
              {fieldErrors.startingPrice && <span className="error-hint">{fieldErrors.startingPrice}</span>}
            </div>

            {/* Minimum Bid Increment */}
            <div className="form-group">
              <label htmlFor="minBidIncrement" className="form-label">
                Min Bid Increment ($ USD) <span className="req">*</span>
              </label>
              <div className="input-prefix-wrapper">
                <span className="input-prefix">$</span>
                <input
                  id="minBidIncrement"
                  type="number"
                  step="0.01"
                  min="0.01"
                  className={`form-input has-prefix ${fieldErrors.minBidIncrement ? 'input-error' : ''}`}
                  placeholder="5.00"
                  value={formData.minBidIncrement}
                  onChange={(e) => setFormData({ ...formData, minBidIncrement: e.target.value })}
                />
              </div>
              {fieldErrors.minBidIncrement && <span className="error-hint">{fieldErrors.minBidIncrement}</span>}
            </div>

            {/* Closing Date & Time */}
            <div className="form-group span-full">
              <label htmlFor="closingTime" className="form-label">
                Auction Closing Date & Time <span className="req">*</span>
              </label>
              <input
                id="closingTime"
                type="datetime-local"
                min={getMinClosingTime()}
                className={`form-input ${fieldErrors.closingTime ? 'input-error' : ''}`}
                value={formData.closingTime}
                onChange={(e) => setFormData({ ...formData, closingTime: e.target.value })}
              />
              <span className="form-hint">Closing time must be set in the future.</span>
              {fieldErrors.closingTime && <span className="error-hint">{fieldErrors.closingTime}</span>}
            </div>

            {/* Photo Upload Section */}
            <div className="form-group span-full">
              <label className="form-label">
                Item Photos (Up to 5 images, max 5MB each)
              </label>

              {/* Upload Dropzone */}
              <div className="upload-dropzone">
                <input
                  type="file"
                  id="photo-upload"
                  className="file-hidden-input"
                  multiple
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  onChange={handleFileChange}
                  disabled={previewUrls.length >= 5}
                />
                <label htmlFor="photo-upload" className="dropzone-label">
                  <span className="upload-icon">📸</span>
                  <span className="upload-title">Click to upload photos from your device</span>
                  <span className="upload-sub">Supports JPEG, PNG, WEBP (Max 5MB each)</span>
                </label>
              </div>

              {/* Or Add by URL */}
              <div className="url-photo-row">
                <input
                  type="url"
                  className="form-input"
                  placeholder="Or paste an image web link (https://...)"
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

              {fieldErrors.photos && <span className="error-hint">{fieldErrors.photos}</span>}

              {/* Photo Previews */}
              {previewUrls.length > 0 && (
                <div className="photo-preview-grid">
                  {previewUrls.map((url, idx) => (
                    <div key={idx} className="preview-thumb">
                      <img src={url} alt={`Preview ${idx + 1}`} />
                      <button
                        type="button"
                        className="thumb-remove-btn"
                        onClick={() => handleRemovePhoto(idx)}
                        title="Remove photo"
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

          {/* Modal Footer */}
          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={isSubmitting}>
              {isSubmitting ? (
                <>
                  <span className="spinner"></span> Creating Listing...
                </>
              ) : (
                'Create Listing'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
