/**
 * CampusBid AI Auction Copilot API client
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

export const copilotApi = {
  /**
   * Send a natural language query or prompt to the Copilot.
   * @param {Object} payload
   * @param {string} payload.message - Prompt / search / question
   * @param {number|null} [payload.contextListingId] - Active lot id if opened from an auction
   * @param {number|null} [payload.maxBudget] - Optional budget cap
   * @param {string} [payload.studentId] - Authenticated student ID
   */
  async sendMessage({ message, contextListingId = null, maxBudget = null, studentId = null }) {
    const res = await fetch('/api/copilot/chat', {
      method: 'POST',
      headers: getHeaders(studentId, true),
      body: JSON.stringify({
        message,
        contextListingId: contextListingId ? Number(contextListingId) : null,
        maxBudget: maxBudget !== null && maxBudget !== undefined && maxBudget !== '' ? Number(maxBudget) : null,
      }),
    });

    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'AI Copilot encountered an error.');
    }
    return data.data;
  },

  /**
   * Explicitly confirm and execute a bid proposal created by the Copilot.
   * Mandated by CampusBid safety policy: Never place bids from chat text alone.
   * @param {Object} payload
   * @param {number} payload.listingId
   * @param {number} payload.bidAmount
   * @param {number|null} [payload.maxBudget]
   * @param {string} payload.studentId
   */
  async confirmBid({ listingId, bidAmount, maxBudget = null, studentId }) {
    if (!studentId) {
      throw new Error('Authentication required: please log in with your Student ID to place bids.');
    }

    const res = await fetch('/api/copilot/confirm-bid', {
      method: 'POST',
      headers: getHeaders(studentId, true),
      body: JSON.stringify({
        listingId: Number(listingId),
        bidAmount: Number(bidAmount),
        maxBudget: maxBudget !== null && maxBudget !== undefined && maxBudget !== '' ? Number(maxBudget) : null,
      }),
    });

    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Bid confirmation failed.');
    }
    return data.data;
  },
};
