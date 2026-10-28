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
