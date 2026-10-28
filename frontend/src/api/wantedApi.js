const BASE_URL = '/api/wanted-items';

export const wantedApi = {
  /**
   * Fetch all wanted item alert requests for the authenticated student.
   */
  async getMyWantedItems() {
    const res = await fetch(BASE_URL, {
      method: 'GET',
      credentials: 'include',
      headers: {
        'Accept': 'application/json',
      },
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch your wanted items.');
    }
    return data.data || [];
  },

  /**
   * Post a new wanted item alert request.
   */
  async createWantedItem(itemData) {
    const res = await fetch(BASE_URL, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(itemData),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to create wanted item alert.');
    }
    return data.data;
  },

  /**
   * Retrieve a single wanted item request by ID.
   */
  async getWantedItemById(id) {
    const res = await fetch(`${BASE_URL}/${id}`, {
      method: 'GET',
      credentials: 'include',
      headers: {
        'Accept': 'application/json',
      },
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to fetch wanted item details.');
    }
    return data.data;
  },

  /**
   * Update an existing wanted item alert.
   */
  async updateWantedItem(id, itemData) {
    const res = await fetch(`${BASE_URL}/${id}`, {
      method: 'PUT',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(itemData),
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to update wanted item.');
    }
    return data.data;
  },

  /**
   * Toggle active/paused status of a wanted item alert.
   */
  async toggleStatus(id, status) {
    const url = status ? `${BASE_URL}/${id}/status?status=${status}` : `${BASE_URL}/${id}/status`;
    const res = await fetch(url, {
      method: 'PATCH',
      credentials: 'include',
      headers: {
        'Accept': 'application/json',
      },
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to toggle alert status.');
    }
    return data.data;
  },

  /**
   * Delete a wanted item alert.
   */
  async deleteWantedItem(id) {
    const res = await fetch(`${BASE_URL}/${id}`, {
      method: 'DELETE',
      credentials: 'include',
      headers: {
        'Accept': 'application/json',
      },
    });
    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Failed to delete wanted item.');
    }
    return data.success;
  },
};
