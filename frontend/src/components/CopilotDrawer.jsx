import { useEffect, useRef, useState } from 'react';
import { copilotApi } from '../api/copilotApi';

const DEFAULT_PROMPTS = [
  'Find me a cycle under ₹2,000',
  'Show electronics auctions closing soon',
  'Which books are in good condition under ₹500?',
  'Explain how bidding works',
  'What is the current highest bid for this item?',
];

export default function CopilotDrawer({
  isOpen,
  onToggle,
  onClose,
  currentStudent,
  contextListing,
  onClearContext,
  onViewLot,
  onBidConfirmed,
}) {
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      role: 'assistant',
      text: `### 👋 Welcome to CampusBid AI Copilot!

I am your campus auction assistant. I can help you:
- **Search active lots** by item keyword, condition, price limit, or category
- **Analyze bidding status** (leading bids, minimum increments, closing deadlines)
- **Suggest bid amounts** tailored to your budget

> 🔒 **Mandatory Confirmation Rule**: I will **never** submit a bid just because an amount was mentioned in chat. Every bid requires your explicit, authenticated confirmation click.`,
      suggestedActions: DEFAULT_PROMPTS,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
  ]);

  const [inputText, setInputText] = useState('');
  const [maxBudget, setMaxBudget] = useState('');
  const [showBudgetField, setShowBudgetField] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [confirmingBidId, setConfirmingBidId] = useState(null);
  const [confirmationStatus, setConfirmationStatus] = useState({});

  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  // Auto-scroll chat to bottom
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
      setTimeout(() => inputRef.current?.focus(), 150);
    }
  }, [isOpen, messages]);

  // When contextListing changes while open, let user know
  useEffect(() => {
    if (contextListing && isOpen) {
      // Don't duplicate if already focused
      setMessages((prev) => {
        const last = prev[prev.length - 1];
        if (last && last.contextLotId === contextListing.id) return prev;
        return [
          ...prev,
          {
            id: `ctx-${contextListing.id}-${Date.now()}`,
            role: 'assistant',
            text: `🎯 **Focused on LOT #${contextListing.id}**: *"${contextListing.title}"*\n- Current Leading Bid: **$${Number(contextListing.currentLeadingBid || contextListing.currentPrice || contextListing.startingPrice).toFixed(2)}**\n- Min Next Bid: **$${Number(contextListing.minNextBid || contextListing.startingPrice).toFixed(2)}**\n\nHow can I help you with this auction?`,
            suggestedActions: [
              `What is the current highest bid for this item?`,
              `Help me bid on Lot #${contextListing.id}`,
              `Explain how bidding works`,
            ],
            contextLotId: contextListing.id,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          },
        ];
      });
    }
  }, [contextListing, isOpen]);

  const handleSendMessage = async (textToSend = null) => {
    const rawMessage = (textToSend || inputText).trim();
    if (!rawMessage || isLoading) return;

    const userMsg = {
      id: `u-${Date.now()}`,
      role: 'user',
      text: rawMessage,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputText('');
    setIsLoading(true);

    try {
      const budgetNum = maxBudget ? parseFloat(maxBudget) : null;
      const response = await copilotApi.sendMessage({
        message: rawMessage,
        contextListingId: contextListing ? contextListing.id : null,
        maxBudget: budgetNum,
        studentId: currentStudent?.id,
      });

      const assistantMsg = {
        id: `a-${Date.now()}`,
        role: 'assistant',
        text: response.reply,
        intent: response.intent,
        matchingListings: response.matchingListings || [],
        bidProposal: response.bidProposal || null,
        suggestedActions: response.suggestedActions || [],
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };

      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err) {
      const errorMsg = {
        id: `err-${Date.now()}`,
        role: 'assistant',
        isError: true,
        text: `⚠️ **Copilot Notice:** ${err.message || 'Unable to process your request at this moment.'}\n\n*Please ensure your request is within 1000 characters or try asking about specific active lots.*`,
        suggestedActions: [
          'Find me a cycle under ₹2,000',
          'Show electronics auctions closing soon',
          'Explain how bidding works',
        ],
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  // Mandatory Confirmation Bid Execution
  const handleConfirmBid = async (proposal, msgId) => {
    if (!proposal || confirmingBidId) return;

    if (!currentStudent?.id) {
      setConfirmationStatus((prev) => ({
        ...prev,
        [msgId]: { error: 'Authentication required. Please select a student profile first.' },
      }));
      return;
    }

    setConfirmingBidId(msgId);
    setConfirmationStatus((prev) => ({
      ...prev,
      [msgId]: { loading: true },
    }));

    try {
      const result = await copilotApi.confirmBid({
        listingId: proposal.listingId,
        bidAmount: proposal.proposedBid,
        maxBudget: proposal.maxBudget,
        studentId: currentStudent.id,
      });

      setConfirmationStatus((prev) => ({
        ...prev,
        [msgId]: {
          success: true,
          message: `Bid of $${Number(proposal.proposedBid).toFixed(2)} placed successfully! You are now the leading bidder.`,
        },
      }));

      // Notify parent to refresh listings and details
      if (onBidConfirmed) {
        onBidConfirmed(result);
      }
    } catch (err) {
      setConfirmationStatus((prev) => ({
        ...prev,
        [msgId]: {
          error: err.message || 'Bid rejected by backend validation.',
        },
      }));
    } finally {
      setConfirmingBidId(null);
    }
  };

  const handleDismissProposal = (msgId) => {
    setConfirmationStatus((prev) => ({
      ...prev,
      [msgId]: { dismissed: true },
    }));
  };

  const handleClearChat = () => {
    setMessages([
      {
        id: 'welcome-reset',
        role: 'assistant',
        text: `### 🧹 Session Cleared\n\nHow can I help you today with CampusBid auctions?`,
        suggestedActions: DEFAULT_PROMPTS,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ]);
    setConfirmationStatus({});
  };

  return (
    <>
      {/* Floating Copilot Launcher Button */}
      {!isOpen && (
        <button
          className="copilot-launcher-btn"
          onClick={onToggle}
          title="Open AI Auction Copilot"
          aria-label="Open AI Auction Copilot"
        >
          <div className="copilot-launcher-icon-wrapper">
            <span className="copilot-launcher-icon">🤖</span>
            <span className="copilot-pulse-ring"></span>
          </div>
          <div className="copilot-launcher-content">
            <span className="copilot-launcher-title">AI Copilot</span>
            <span className="copilot-launcher-subtitle">
              {contextListing ? `Lot #${contextListing.id}` : 'Fair Auction Assistant'}
            </span>
          </div>
        </button>
      )}

      {/* Floating / Slide-out Copilot Panel */}
      {isOpen && (
        <aside className="copilot-drawer-panel" aria-label="CampusBid AI Auction Copilot">
          {/* Header */}
          <div className="copilot-header">
            <div className="copilot-brand-group">
              <div className="copilot-avatar">
                <span>🤖</span>
                <span className="copilot-online-dot" title="Active Copilot Assistant"></span>
              </div>
              <div className="copilot-header-text">
                <div className="copilot-header-title-row">
                  <h3 className="copilot-title">CampusBid Copilot</h3>
                  <span className="copilot-badge">AI Assistant</span>
                </div>
                <span className="copilot-status-sub">Fair Bidding Guard • Active Catalog</span>
              </div>
            </div>

            <div className="copilot-header-actions">
              <button
                type="button"
                className="copilot-header-icon-btn"
                onClick={handleClearChat}
                title="Clear conversation history"
              >
                🧹
              </button>
              <button
                type="button"
                className="copilot-header-icon-btn close-btn"
                onClick={onClose}
                title="Close Copilot Panel"
              >
                ✕
              </button>
            </div>
          </div>

          {/* Context Banner if opened from a specific lot */}
          {contextListing && (
            <div className="copilot-context-bar">
              <div className="copilot-context-info">
                <span className="copilot-context-pin">🎯</span>
                <span className="copilot-context-label">Focus Lot:</span>
                <strong className="copilot-context-title">
                  #{contextListing.id} &mdash; {contextListing.title}
                </strong>
              </div>
              {onClearContext && (
                <button
                  type="button"
                  className="copilot-context-clear"
                  onClick={onClearContext}
                  title="Remove focus lot"
                >
                  ✕
                </button>
              )}
            </div>
          )}

          {/* Chat Messages Body */}
          <div className="copilot-body">
            {messages.map((msg) => {
              const status = confirmationStatus[msg.id] || {};
              const proposal = msg.bidProposal;

              return (
                <div
                  key={msg.id}
                  className={`copilot-message-row ${msg.role === 'user' ? 'row-user' : 'row-assistant'} ${msg.isError ? 'row-error' : ''}`}
                >
                  {msg.role === 'assistant' && (
                    <div className="copilot-msg-avatar">
                      <span>🤖</span>
                    </div>
                  )}

                  <div className={`copilot-bubble ${msg.role === 'user' ? 'bubble-user' : 'bubble-assistant'}`}>
                    {/* Render message text with basic formatting */}
                    <div className="copilot-msg-content">
                      {msg.text.split('\n\n').map((paragraph, idx) => {
                        // Headers
                        if (paragraph.startsWith('### ')) {
                          return (
                            <h4 key={idx} className="copilot-heading">
                              {paragraph.replace('### ', '')}
                            </h4>
                          );
                        }
                        // Blockquote
                        if (paragraph.startsWith('> ')) {
                          return (
                            <blockquote key={idx} className="copilot-quote">
                              {paragraph.replace(/^>\s*/gm, '')}
                            </blockquote>
                          );
                        }
                        // Bullet lists
                        if (paragraph.includes('\n- ') || paragraph.startsWith('- ')) {
                          const lines = paragraph.split('\n');
                          return (
                            <ul key={idx} className="copilot-list">
                              {lines.map((line, lIdx) => {
                                if (line.startsWith('- ')) {
                                  return (
                                    <li key={lIdx} dangerouslySetInnerHTML={{ __html: formatInline(line.replace('- ', '')) }} />
                                  );
                                }
                                return (
                                  <p key={lIdx} dangerouslySetInnerHTML={{ __html: formatInline(line) }} />
                                );
                              })}
                            </ul>
                          );
                        }
                        // Regular text
                        return (
                          <p key={idx} dangerouslySetInnerHTML={{ __html: formatInline(paragraph) }} />
                        );
                      })}
                    </div>

                    {/* Matching Listings Cards inside Chat */}
                    {msg.matchingListings && msg.matchingListings.length > 0 && (
                      <div className="copilot-matches-container">
                        <div className="copilot-matches-header">
                          <span>📦 Matching Campus Auctions ({msg.matchingListings.length})</span>
                        </div>
                        <div className="copilot-matches-list">
                          {msg.matchingListings.map((item) => {
                            const leading = item.currentLeadingBid || item.currentPrice || item.startingPrice;
                            const photo = item.photoUrls && item.photoUrls.length > 0
                              ? item.photoUrls[0]
                              : null;

                            return (
                              <div key={item.id} className="copilot-listing-card">
                                <div className="copilot-card-thumb">
                                  {photo ? (
                                    <img src={photo} alt={item.title} />
                                  ) : (
                                    <div className="copilot-no-thumb">🏷️</div>
                                  )}
                                  <span className="copilot-card-lot">LOT #{item.id}</span>
                                </div>
                                <div className="copilot-card-info">
                                  <h5 className="copilot-card-title" title={item.title}>
                                    {item.title}
                                  </h5>
                                  <div className="copilot-card-meta">
                                    <span className="copilot-card-tag">{item.category}</span>
                                    <span className="copilot-card-tag tag-cond">{item.condition}</span>
                                  </div>
                                  <div className="copilot-card-price-row">
                                    <div className="copilot-price-box">
                                      <span className="copilot-price-label">Current Bid</span>
                                      <span className="copilot-price-val">${Number(leading).toFixed(2)}</span>
                                    </div>
                                    <button
                                      type="button"
                                      className="btn-view-lot"
                                      onClick={() => onViewLot && onViewLot(item)}
                                      title="Open auction details & bid"
                                    >
                                      👁️ View Lot
                                    </button>
                                  </div>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {/* Mandatory Bid Confirmation Card */}
                    {proposal && !status.dismissed && (
                      <div className="copilot-bid-confirmation-box">
                        <div className="confirm-box-header">
                          <span className="confirm-shield-icon">🔒</span>
                          <div className="confirm-header-text">
                            <strong>Mandatory Bid Confirmation</strong>
                            <span>CampusBid Safety Rule: Explicit Approval Required</span>
                          </div>
                        </div>

                        <div className="confirm-specs-grid">
                          <div className="confirm-spec-item">
                            <span className="spec-label">Lot:</span>
                            <span className="spec-value">#{proposal.listingId} &mdash; {proposal.listingTitle}</span>
                          </div>
                          <div className="confirm-spec-item">
                            <span className="spec-label">Current Leading:</span>
                            <span className="spec-value">${Number(proposal.currentLeadingBid).toFixed(2)}</span>
                          </div>
                          <div className="confirm-spec-item">
                            <span className="spec-label">Min Increment:</span>
                            <span className="spec-value">+${Number(proposal.minBidIncrement).toFixed(2)}</span>
                          </div>
                          <div className="confirm-spec-item highlight-next">
                            <span className="spec-label">Proposed Bid:</span>
                            <span className="spec-value amount-orange">
                              ${Number(proposal.proposedBid).toFixed(2)}
                            </span>
                          </div>
                          {proposal.maxBudget && (
                            <div className="confirm-spec-item">
                              <span className="spec-label">Your Budget Cap:</span>
                              <span className="spec-value">${Number(proposal.maxBudget).toFixed(2)}</span>
                            </div>
                          )}
                        </div>

                        {proposal.warning && (
                          <div className="confirm-warning-note">
                            <span>⚠️ {proposal.warning}</span>
                          </div>
                        )}

                        {/* Confirmation Status / Result Feedback */}
                        {status.success && (
                          <div className="confirm-feedback-alert feedback-success">
                            <span>🎉 {status.message}</span>
                          </div>
                        )}

                        {status.error && (
                          <div className="confirm-feedback-alert feedback-error">
                            <span>❌ {status.error}</span>
                          </div>
                        )}

                        {/* Confirmation Buttons (Only if not already confirmed or dismissed) */}
                        {!status.success && (
                          <div className="confirm-actions-row">
                            <button
                              type="button"
                              className="btn-confirm-bid-action"
                              disabled={!proposal.canAfford || confirmingBidId === msg.id}
                              onClick={() => handleConfirmBid(proposal, msg.id)}
                            >
                              {confirmingBidId === msg.id ? (
                                <span>Submitting Bid...</span>
                              ) : (
                                <span>✅ Confirm & Place Bid (${Number(proposal.proposedBid).toFixed(2)})</span>
                              )}
                            </button>
                            <button
                              type="button"
                              className="btn-cancel-proposal"
                              onClick={() => handleDismissProposal(msg.id)}
                              disabled={confirmingBidId === msg.id}
                            >
                              Dismiss
                            </button>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Suggested Action Chips */}
                    {msg.suggestedActions && msg.suggestedActions.length > 0 && (
                      <div className="copilot-action-chips">
                        {msg.suggestedActions.map((action, aIdx) => (
                          <button
                            key={aIdx}
                            type="button"
                            className="copilot-chip"
                            onClick={() => handleSendMessage(action)}
                            disabled={isLoading}
                          >
                            {action}
                          </button>
                        ))}
                      </div>
                    )}

                    <span className="copilot-timestamp">{msg.timestamp}</span>
                  </div>
                </div>
              );
            })}

            {/* Loading Indicator */}
            {isLoading && (
              <div className="copilot-message-row row-assistant">
                <div className="copilot-msg-avatar">
                  <span>🤖</span>
                </div>
                <div className="copilot-bubble bubble-assistant copilot-loading-bubble">
                  <div className="typing-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                  <span className="typing-text">Analyzing active campus auctions...</span>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Footer Controls & Input */}
          <div className="copilot-footer">
            {/* Optional Budget Ceiling Toggle */}
            <div className="copilot-footer-toolbar">
              <button
                type="button"
                className={`budget-toggle-btn ${showBudgetField || maxBudget ? 'active' : ''}`}
                onClick={() => setShowBudgetField(!showBudgetField)}
                title="Specify your maximum spending budget"
              >
                <span>💰 Max Budget {maxBudget ? `(₹${maxBudget})` : ''}</span>
              </button>
              {currentStudent && (
                <span className="copilot-auth-badge" title={`Authenticated as ${currentStudent.name} (${currentStudent.id})`}>
                  👤 {currentStudent.id}
                </span>
              )}
            </div>

            {/* Budget Input Row (when expanded) */}
            {showBudgetField && (
              <div className="copilot-budget-input-row">
                <label htmlFor="copilot-budget-field" className="budget-label">
                  Maximum Budget Ceiling:
                </label>
                <div className="budget-input-wrapper">
                  <span className="budget-currency">₹ / $</span>
                  <input
                    id="copilot-budget-field"
                    type="number"
                    min="1"
                    step="1"
                    placeholder="e.g. 2000"
                    value={maxBudget}
                    onChange={(e) => setMaxBudget(e.target.value)}
                    className="budget-input"
                  />
                  {maxBudget && (
                    <button
                      type="button"
                      className="budget-clear-btn"
                      onClick={() => setMaxBudget('')}
                    >
                      ✕
                    </button>
                  )}
                </div>
              </div>
            )}

            {/* Message Input Bar */}
            <div className="copilot-input-bar">
              <input
                ref={inputRef}
                type="text"
                className="copilot-text-input"
                placeholder="Ask about auctions, search items, or ask for bid advice..."
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={handleKeyDown}
                maxLength={1000}
                disabled={isLoading}
              />
              <button
                type="button"
                className="copilot-send-btn"
                onClick={() => handleSendMessage()}
                disabled={!inputText.trim() || isLoading}
                title="Send message to AI Copilot"
              >
                <span>Send</span>
                <span className="send-arrow">➔</span>
              </button>
            </div>
            <div className="copilot-security-footer-note">
              <span>🔒 AI never bids automatically. Verified bids require manual confirmation.</span>
            </div>
          </div>
        </aside>
      )}
    </>
  );
}

// Utility to format bold, code, and links in plain strings
function formatInline(text) {
  if (!text) return '';
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>');
}
