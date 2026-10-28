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

/**
 * Custom fetch wrapper with credentials included for secure session management.
 */
function apiFetch(url, options = {}) {
  return fetch(url, {
    credentials: 'include',
    ...options,
  });
}

function buildListingParams(filterOptions = {}) {
  const params = new URLSearchParams();
  const {
    status,
    category,
    condition,
    minPrice,
    maxPrice,
    closingSoon,
    keyword,
    sortBy,
  } = filterOptions;

  if (status && status !== 'ALL') params.append('status', status);
  if (category && category !== 'All') params.append('category', category);
  if (condition && condition !== 'ALL') params.append('condition', condition);
  if (minPrice !== undefined && minPrice !== null && minPrice !== '') params.append('minPrice', minPrice);
  if (maxPrice !== undefined && maxPrice !== null && maxPrice !== '') params.append('maxPrice', maxPrice);
  if (closingSoon) params.append('closingSoon', 'true');
  if (keyword && typeof keyword === 'string' && keyword.trim()) params.append('keyword', keyword.trim());
  if (sortBy) params.append('sortBy', sortBy);

  return params;
}

export const listingApi = {
  /**
   * Fetch active listings with multi-criteria filters and keyword search.
   */
  async getActiveListings(filtersOrCategory = {}, maybeKeyword = '') {
    const opts = typeof filtersOrCategory === 'object' && filtersOrCategory !== null
      ? filtersOrCategory
      : { category: filtersOrCategory, keyword: maybeKeyword };

    const params = buildListingParams(opts);
    const url = `/api/listings${params.toString() ? '?' + params.toString() : ''}`;
    const res = await apiFetch(url);
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch active listings');
    }
    return data.data || [];
  },

  /**
   * Fetch all listings including CANCELLED and ENDED with multi-criteria filters.
   */
  async getAllListings(filtersOrStatus = {}, maybeCategory = '', maybeKeyword = '') {
    const opts = typeof filtersOrStatus === 'object' && filtersOrStatus !== null
      ? filtersOrStatus
      : { status: filtersOrStatus, category: maybeCategory, keyword: maybeKeyword };

    const params = buildListingParams(opts);
    const url = `/api/listings/all${params.toString() ? '?' + params.toString() : ''}`;
    const res = await apiFetch(url);
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
    const res = await apiFetch(`/api/listings/${id}`);
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
    const res = await apiFetch('/api/listings', {
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
    const res = await apiFetch(`/api/listings/${id}`, {
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
    const res = await apiFetch(`/api/listings/${id}/cancel`, {
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
    const res = await apiFetch('/api/listings/my-listings', {
      headers: getHeaders(studentId, false),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch your listings');
    }
    return data.data || [];
  },

  /**
   * Place a bid on an active listing.
   */
  async placeBid(listingId, bidData, studentId) {
    const res = await apiFetch(`/api/listings/${listingId}/bids`, {
      method: 'POST',
      headers: getHeaders(studentId, true),
      body: JSON.stringify(bidData),
    });
    const data = await res.json();
    if (!res.ok) {
      const err = new Error(data.message || 'Failed to place bid');
      err.fieldErrors = data.errors;
      throw err;
    }
    return data.data;
  },

  /**
   * Fetch public bid history for an auction.
   */
  async getBidHistory(listingId) {
    const res = await apiFetch(`/api/listings/${listingId}/bids`);
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch bid history');
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

    const res = await apiFetch('/api/listings/upload-images', {
      method: 'POST',
      body: formData,
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to upload images');
    }
    return data.data || [];
  },

  /**
   * Fetch closed listings awaiting seller decision for active student.
   */
  async getPendingDecisions(sellerId) {
    const res = await apiFetch('/api/listings/pending-decisions', {
      headers: getHeaders(sellerId, false),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch pending decisions');
    }
    return data.data || [];
  },

  /**
   * Fetch listings won by active student.
   */
  async getWonListings(studentId) {
    const res = await apiFetch('/api/listings/won', {
      headers: getHeaders(studentId, false),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch won listings');
    }
    return data.data || [];
  },

  /**
   * Submit seller decision (APPROVE or REJECT) on winning bid.
   */
  async submitDecision(listingId, decision, notes, sellerId) {
    const res = await apiFetch(`/api/listings/${listingId}/decision`, {
      method: 'POST',
      headers: getHeaders(sellerId, true),
      body: JSON.stringify({ decision, notes }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to submit decision');
    }
    return data.data;
  },

  /**
   * Convenience shortcut to approve winning bid.
   */
  async approveBid(listingId, notes, sellerId) {
    const res = await apiFetch(`/api/listings/${listingId}/approve`, {
      method: 'POST',
      headers: getHeaders(sellerId, true),
      body: JSON.stringify({ notes }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to approve winning bid');
    }
    return data.data;
  },

  /**
   * Convenience shortcut to reject winning bid.
   */
  async rejectBid(listingId, notes, sellerId) {
    const res = await apiFetch(`/api/listings/${listingId}/reject`, {
      method: 'POST',
      headers: getHeaders(sellerId, true),
      body: JSON.stringify({ notes }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to reject winning bid');
    }
    return data.data;
  },

  /**
   * Confirm campus handover by seller or winning bidder.
   */
  async confirmHandover(listingId, studentId) {
    const res = await apiFetch(`/api/listings/${listingId}/confirm-handover`, {
      method: 'POST',
      headers: getHeaders(studentId, false),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to confirm handover');
    }
    return data.data;
  },

  /**
   * Finalize campus handover directly.
   */
  async finalizeHandover(listingId, studentId) {
    const res = await apiFetch(`/api/listings/${listingId}/finalize-handover`, {
      method: 'POST',
      headers: getHeaders(studentId, false),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to finalize handover');
    }
    return data.data;
  },

  /**
   * Cancel approved sale or record failed pickup.
   */
  async cancelSale(listingId, reason, studentId) {
    const res = await apiFetch(`/api/listings/${listingId}/cancel-sale`, {
      method: 'POST',
      headers: getHeaders(studentId, true),
      body: JSON.stringify({ reason }),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to cancel sale');
    }
    return data.data;
  },

  /**
   * Manually close an active auction.
   */
  async closeAuction(listingId, sellerId) {
    const res = await apiFetch(`/api/listings/${listingId}/close`, {
      method: 'POST',
      headers: getHeaders(sellerId, false),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to close auction');
    }
    return data.data;
  },
};
