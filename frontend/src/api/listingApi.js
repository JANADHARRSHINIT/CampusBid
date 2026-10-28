/**
 * Helper to build headers with student ID for ownership security checks.
 */
function getHeaders(studentId, isJson = true) {
  const headers = {};
  if (isJson) {
    headers['Content-Type'] = 'application/json';
  }
  if (studentId) {
    headers['X-Student-Id'] = studentId;
  }
  return headers;
}

export const listingApi = {
  /**
   * Fetch active listings with optional category and search keyword.
   */
  async getActiveListings(category = '', keyword = '') {
    const params = new URLSearchParams();
    if (category && category !== 'All') params.append('category', category);
    if (keyword && keyword.trim()) params.append('keyword', keyword.trim());

    const url = `/api/listings${params.toString() ? '?' + params.toString() : ''}`;
    const res = await fetch(url);
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch active listings');
    }
    return data.data || [];
  },

  /**
   * Fetch all listings including CANCELLED and ENDED.
   */
  async getAllListings(status = '', category = '', keyword = '') {
    const params = new URLSearchParams();
    if (status && status !== 'ALL') params.append('status', status);
    if (category && category !== 'All') params.append('category', category);
    if (keyword && keyword.trim()) params.append('keyword', keyword.trim());

    const url = `/api/listings/all${params.toString() ? '?' + params.toString() : ''}`;
    const res = await fetch(url);
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch listings');
    }
    return data.data || [];
  },

  /**
   * Fetch a single listing by its ID.
   */
  async getListingById(id) {
    const res = await fetch(`/api/listings/${id}`);
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch listing');
    }
    return data.data;
  },

  /**
   * Create a new item listing.
   */
  async createListing(listingData, studentId) {
    const res = await fetch('/api/listings', {
      method: 'POST',
      headers: getHeaders(studentId, true),
      body: JSON.stringify(listingData),
    });
    const data = await res.json();
    if (!res.ok) {
      const err = new Error(data.message || 'Failed to create listing');
      err.fieldErrors = data.errors;
      throw err;
    }
    return data.data;
  },

  /**
   * Update an existing listing (restricted to seller).
   */
  async updateListing(id, listingData, studentId) {
    const res = await fetch(`/api/listings/${id}`, {
      method: 'PUT',
      headers: getHeaders(studentId, true),
      body: JSON.stringify(listingData),
    });
    const data = await res.json();
    if (!res.ok) {
      const err = new Error(data.message || 'Failed to update listing');
      err.fieldErrors = data.errors;
      throw err;
    }
    return data.data;
  },

  /**
   * Cancel an existing listing after confirmation (restricted to seller).
   */
  async cancelListing(id, studentId) {
    const res = await fetch(`/api/listings/${id}/cancel`, {
      method: 'POST',
      headers: getHeaders(studentId, false),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to cancel listing');
    }
    return data.data;
  },

  /**
   * Fetch listings created by a specific student.
   */
  async getMyListings(studentId) {
    const res = await fetch('/api/listings/my-listings', {
      headers: getHeaders(studentId, false),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch your listings');
    }
    return data.data || [];
  },

  /**
   * Upload multiple photos (validates type and size).
   */
  async uploadImages(files) {
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
      formData.append('files', files[i]);
    }

    const res = await fetch('/api/listings/upload-images', {
      method: 'POST',
      body: formData,
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to upload images');
    }
    return data.data || [];
  },
};
