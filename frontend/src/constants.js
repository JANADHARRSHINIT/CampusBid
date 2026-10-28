export const DEMO_STUDENTS = [
  { id: 'student-101', name: 'Alex Chen', email: 'alex.chen@campus.edu', avatar: '👨‍🎓' },
  { id: 'student-102', name: 'Maya Patel', email: 'maya.patel@campus.edu', avatar: '👩‍🎓' },
  { id: 'student-103', name: 'Rohan Sharma', email: 'rohan.sharma@campus.edu', avatar: '🧑‍💻' },
];

export const CATEGORIES = [
  'All',
  'Textbooks',
  'Electronics',
  'Dorm & Furniture',
  'Bikes & Transport',
  'Apparel',
  'Sports & Hobbies',
  'Other',
];

export const CATEGORY_ICONS = {
  All: '🏷️',
  Textbooks: '📚',
  Electronics: '💻',
  'Dorm & Furniture': '🛋️',
  'Bikes & Transport': '🚲',
  Apparel: '👕',
  'Sports & Hobbies': '⚽',
  Other: '📦',
};

export const CONDITIONS = [
  { value: 'NEW', label: 'Brand New (Unopened/Unused)' },
  { value: 'LIKE_NEW', label: 'Like New (Minimal signs of use)' },
  { value: 'GOOD', label: 'Good (Working properly with minor cosmetic wear)' },
  { value: 'FAIR', label: 'Fair (Functional with noticeable wear)' },
  { value: 'POOR', label: 'Poor / For Parts' },
];

export const CONDITION_COLORS = {
  NEW: { label: 'Mint / Unused', class: 'tag-stamp stamp-mint' },
  LIKE_NEW: { label: 'Like New', class: 'tag-stamp stamp-forest' },
  GOOD: { label: 'Good Condition', class: 'tag-stamp stamp-sepia' },
  FAIR: { label: 'Fair / Field-Tested', class: 'tag-stamp stamp-amber' },
  POOR: { label: 'As-Is / Vintage', class: 'tag-stamp stamp-rust' },
};

export const STATUS_META = {
  ACTIVE: { label: '🔥 Active', class: 'status-active', color: '#2d6a4f' },
  AWAITING_SELLER_APPROVAL: { label: '⌛ Awaiting Seller Decision', class: 'status-awaiting', color: '#d97706' },
  APPROVED: { label: '✅ Sale Approved / Reserved', class: 'status-approved', color: '#16a34a' },
  REJECTED: { label: '❌ Bid Rejected', class: 'status-rejected', color: '#dc2626' },
  UNSOLD: { label: '🚫 Unsold (No Bids)', class: 'status-unsold', color: '#64748b' },
  COMPLETED: { label: '🎉 Handover Complete', class: 'status-completed', color: '#059669' },
  CANCELLED: { label: '🚫 Cancelled', class: 'status-cancelled', color: '#991b1b' },
  CLOSED: { label: '⌛ Auction Closed', class: 'status-ended', color: '#64748b' },
  ENDED: { label: '⌛ Auction Closed', class: 'status-ended', color: '#64748b' },
};

export function getStatusBadge(status, isExpired, hasBids = false) {
  if (status === 'ACTIVE' && isExpired) {
    return hasBids ? STATUS_META.AWAITING_SELLER_APPROVAL : STATUS_META.UNSOLD;
  }
  return STATUS_META[status] || { label: status || 'Closed', class: 'status-ended', color: '#64748b' };
}

export function calculateTimeRemaining(closingTimeStr) {
  if (!closingTimeStr) return { text: 'Auction Closed', isExpired: true };
  const closing = new Date(closingTimeStr).getTime();
  const now = Date.now();
  const diffMs = closing - now;
  if (diffMs <= 0) return { text: 'Auction Closed', isExpired: true };

  const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  const minutes = Math.floor((diffMs % (1000 * 60)) / (1000 * 60));
  const seconds = Math.floor((diffMs % 1000) / 1000);

  if (days > 0) return { text: `${days}d ${hours}h left`, isExpired: false };
  if (hours > 0) return { text: `${hours}h ${minutes}m left`, isExpired: false };
  if (minutes > 0) return { text: `${minutes}m ${seconds}s left`, isExpired: false };
  return { text: `${seconds}s left`, isExpired: false };
}

export function formatDate(dateStr) {
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

export function formatFullDate(dateStr) {
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
