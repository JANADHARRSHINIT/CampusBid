import { useState, useEffect } from 'react';
import { CATEGORIES } from '../constants';
import { wantedApi } from '../api/wantedApi';

const FORM_CATEGORIES = CATEGORIES.filter((c) => c !== 'All');

export default function WantedItemModal({
  isOpen,
  onClose,
  initialItem = null,
  onSuccess,
  showToast,
}) {
  const isEditing = Boolean(initialItem);

  const [formData, setFormData] = useState({
    itemName: '',
    category: 'Textbooks',
    maxBudget: '',
    preferredCondition: 'ANY',
    additionalRequirements: '',
  });

  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (initialItem) {
      setFormData({
        itemName: initialItem.itemName || '',
        category: initialItem.category || 'Textbooks',
        maxBudget: initialItem.maxBudget ? String(initialItem.maxBudget) : '',
        preferredCondition: initialItem.preferredCondition || 'ANY',
        additionalRequirements: initialItem.additionalRequirements || '',
      });
    } else {
      setFormData({
        itemName: '',
        category: 'Textbooks',
        maxBudget: '',
        preferredCondition: 'ANY',
        additionalRequirements: '',
      });
    }
    setErrors({});
  }, [initialItem, isOpen]);

  if (!isOpen) return null;

  const validate = () => {
    const errs = {};
    if (!formData.itemName.trim()) {
      errs.itemName = 'Please enter an item name or search keywords.';
    } else if (formData.itemName.trim().length < 2) {
      errs.itemName = 'Item name must be at least 2 characters.';
    }

    const budgetNum = parseFloat(formData.maxBudget);
    if (!formData.maxBudget || isNaN(budgetNum) || budgetNum <= 0) {
      errs.maxBudget = 'Please enter a valid maximum budget greater than ₹0.';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    try {
      const payload = {
        itemName: formData.itemName.trim(),
        category: formData.category,
        maxBudget: parseFloat(formData.maxBudget),
        preferredCondition: formData.preferredCondition,
        additionalRequirements: formData.additionalRequirements.trim() || undefined,
      };

      if (isEditing) {
        const updated = await wantedApi.updateWantedItem(initialItem.id, payload);
        showToast(`Wanted request for "${updated.itemName}" updated!`, 'success');
        onSuccess(updated);
      } else {
        const created = await wantedApi.createWantedItem(payload);
        showToast(`🎯 Wanted alert for "${created.itemName}" created! You will be notified on new matches.`, 'success');
        onSuccess(created);
      }
      onClose();
    } catch (err) {
      showToast(err.message || 'Failed to save wanted item request.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose} role="dialog" aria-modal="true">
      <div className="modal-container wanted-modal" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="modal-header">
          <div className="modal-title-group">
            <span className="modal-header-icon">🎯</span>
            <div>
              <h2 className="modal-title">
                {isEditing ? 'Edit Wanted Item Alert' : 'Post What You Need'}
              </h2>
              <p className="modal-subtitle">
                CampusBid monitors new auctions 24/7 and alerts you when a matching lot is listed
              </p>
            </div>
          </div>
          <button type="button" className="modal-close-btn" onClick={onClose} title="Close">
            ✕
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="modal-form wanted-form">
          <div className="form-group">
            <label className="form-label" htmlFor="wanted-item-name">
              Item Name or Keywords <span className="req">*</span>
            </label>
            <input
              id="wanted-item-name"
              type="text"
              className={`form-input ${errors.itemName ? 'input-error' : ''}`}
              placeholder="e.g. Hero Sprint Bicycle, Calculus 8th Edition, Mini Fridge"
              value={formData.itemName}
              onChange={(e) => {
                setFormData({ ...formData, itemName: e.target.value });
                if (errors.itemName) setErrors({ ...errors, itemName: '' });
              }}
              required
            />
            {errors.itemName && <span className="field-error-msg">{errors.itemName}</span>}
            <span className="field-hint">
              Be specific with keywords so matching alerts are accurate (e.g. "Bicycle", "Monitor", "Headphones").
            </span>
          </div>

          <div className="form-row-2">
            <div className="form-group">
              <label className="form-label" htmlFor="wanted-category">
                Category
              </label>
              <select
                id="wanted-category"
                className="form-select"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              >
                {FORM_CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="wanted-max-budget">
                Maximum Budget (₹ INR) <span className="req">*</span>
              </label>
              <div className="budget-input-wrapper">
                <span className="currency-prefix">₹</span>
                <input
                  id="wanted-max-budget"
                  type="number"
                  step="1"
                  min="1"
                  className={`form-input budget-input ${errors.maxBudget ? 'input-error' : ''}`}
                  placeholder="2000"
                  value={formData.maxBudget}
                  onChange={(e) => {
                    setFormData({ ...formData, maxBudget: e.target.value });
                    if (errors.maxBudget) setErrors({ ...errors, maxBudget: '' });
                  }}
                  required
                />
              </div>
              {errors.maxBudget && <span className="field-error-msg">{errors.maxBudget}</span>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="wanted-condition">
              Minimum Acceptable Condition
            </label>
            <select
              id="wanted-condition"
              className="form-select"
              value={formData.preferredCondition}
              onChange={(e) => setFormData({ ...formData, preferredCondition: e.target.value })}
            >
              <option value="ANY">Any Condition (Best chance for matches)</option>
              <option value="BRAND_NEW">Brand New Only</option>
              <option value="LIKE_NEW">Like New or Better</option>
              <option value="GOOD">Good Condition or Better</option>
              <option value="FAIR">Fair Condition or Better</option>
            </select>
            <span className="field-hint">
              Choosing "Any Condition" alerts you to all budget-matching items.
            </span>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="wanted-requirements">
              Additional Requirements or Notes (Optional)
            </label>
            <textarea
              id="wanted-requirements"
              className="form-textarea"
              rows={2}
              placeholder="e.g. Gear cycle preferred, must include charger/cable, willing to meet at North Gate."
              value={formData.additionalRequirements}
              onChange={(e) => setFormData({ ...formData, additionalRequirements: e.target.value })}
            />
          </div>

          <div className="wanted-alert-preview-note">
            <span className="note-icon">💡</span>
            <span>
              Once posted, CampusBid automatically checks all newly published auctions. You will receive an immediate in-app notification and email when a classmate lists a matching lot under <strong>₹{formData.maxBudget || '0'}</strong>.
            </span>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? (
                <span>Saving Alert...</span>
              ) : isEditing ? (
                <span>Update Wanted Alert</span>
              ) : (
                <span>🎯 Start Smart Alert Monitoring</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
