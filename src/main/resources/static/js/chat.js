let sessionId = localStorage.getItem('agrichat_session_id') || null;
let isLoading = false;

// ---- Initialization ----
document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('messageInput');
    input.focus();
    updateSessionStatus();
});

// ---- Session Management ----
function updateSessionStatus() {
    const el = document.getElementById('session-status');
    el.textContent = sessionId ? 'Session Active' : 'New Session';
}

function startNewChat() {
    sessionId = null;
    localStorage.removeItem('agrichat_session_id');
    document.getElementById('messagesContainer').innerHTML = '';
    document.getElementById('welcomeState') && document.getElementById('welcomeState').remove();
    showWelcomeState();
    updateSessionStatus();
    hideCategoryStrip();
}

function showWelcomeState() {
    const container = document.getElementById('messagesContainer');
    if (container.querySelector('.welcome-state')) return;
    const div = document.createElement('div');
    div.className = 'welcome-state';
    div.id = 'welcomeState';
    div.innerHTML = `
        <div class="welcome-icon">🌾</div>
        <h2 class="welcome-title">Namaste, Farmer!</h2>
        <p class="welcome-text">Ask me anything about your crops — diseases, weather, fertilizers, or market prices.</p>
        <div class="welcome-chips">
            <button class="chip" onclick="sendQuickMessage('How do I identify and treat wheat rust disease?')">Wheat rust treatment</button>
            <button class="chip" onclick="sendQuickMessage('Best practices for drip irrigation in summer?')">Drip irrigation tips</button>
            <button class="chip" onclick="sendQuickMessage('Organic pesticides for vegetable crops?')">Organic pesticides</button>
            <button class="chip" onclick="sendQuickMessage('What government schemes are available for farmers?')">Govt schemes</button>
        </div>`;
    container.appendChild(div);
}

// ---- Message Sending ----
function sendQuickMessage(text) {
    document.getElementById('messageInput').value = text;
    sendMessage();
}

async function sendMessage() {
    if (isLoading) return;

    const input = document.getElementById('messageInput');
    const message = input.value.trim();
    if (!message) return;

    // Hide welcome state
    const welcomeState = document.getElementById('welcomeState');
    if (welcomeState) welcomeState.style.display = 'none';

    // Show user message
    appendMessage('user', message);
    input.value = '';
    autoResize(input);

    // Show typing indicator
    const typingId = showTypingIndicator();
    setLoading(true);

    try {
        const response = await fetch('/api/chat/message', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: message,
                sessionId: sessionId,
                userName: 'Farmer'
            })
        });

        const data = await response.json();
        removeTypingIndicator(typingId);

        if (data.success) {
            // Save session ID for context
            sessionId = data.sessionId;
            localStorage.setItem('agrichat_session_id', sessionId);
            updateSessionStatus();

            appendMessage('assistant', data.reply, data.category, data.timestamp);
            showCategoryStrip(data.category);
        } else {
            appendErrorMessage(data.errorMessage || 'Something went wrong. Please try again.');
        }
    } catch (err) {
        removeTypingIndicator(typingId);
        appendErrorMessage('Network error. Please check your connection and try again.');
        console.error('Chat error:', err);
    } finally {
        setLoading(false);
    }
}

// ---- DOM Helpers ----
function appendMessage(role, content, category = null, timestamp = null) {
    const container = document.getElementById('messagesContainer');

    const row = document.createElement('div');
    row.className = `message-row ${role}`;

    const wrapper = document.createElement('div');
    wrapper.className = 'bubble-wrapper';

    const bubble = document.createElement('div');
    bubble.className = 'bubble';
    bubble.textContent = content; // Safe: no innerHTML injection

    wrapper.appendChild(bubble);

    // Meta line (time + category tag)
    const meta = document.createElement('div');
    meta.className = 'bubble-meta';

    const timeStr = timestamp
        ? new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    meta.textContent = timeStr;

    if (category && role === 'assistant') {
        const tag = document.createElement('span');
        tag.className = 'category-tag';
        tag.textContent = formatCategory(category);
        meta.appendChild(tag);
    }

    wrapper.appendChild(meta);
    row.appendChild(wrapper);
    container.appendChild(row);
    scrollToBottom();
}

function appendErrorMessage(text) {
    const container = document.getElementById('messagesContainer');
    const row = document.createElement('div');
    row.className = 'message-row assistant';
    row.innerHTML = `<div class="bubble-wrapper">
        <div class="bubble" style="background:#fff5f5;border-color:#fed7d7;color:#c53030;">
            ⚠️ ${text}
        </div></div>`;
    container.appendChild(row);
    scrollToBottom();
}

function showTypingIndicator() {
    const id = 'typing-' + Date.now();
    const container = document.getElementById('messagesContainer');
    const row = document.createElement('div');
    row.className = 'message-row assistant';
    row.id = id;
    row.innerHTML = `<div class="bubble-wrapper">
        <div class="typing-indicator">
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
        </div></div>`;
    container.appendChild(row);
    scrollToBottom();
    return id;
}

function removeTypingIndicator(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
}

// ---- Category Strip ----
const categoryLabels = {
    'CROP_DISEASE':  '🦠 Crop Disease Advice',
    'WEATHER':       '🌤️ Weather & Season Tips',
    'FERTILIZER':    '🌱 Fertilizer & Pesticides',
    'MARKET_PRICE':  '📈 Market Price Info',
    'GENERAL':       '🌾 General Farming'
};

function showCategoryStrip(category) {
    const strip = document.getElementById('categoryStrip');
    const label = document.getElementById('categoryLabel');
    strip.style.display = 'block';
    label.textContent = categoryLabels[category] || '🌾 Farming Advice';
}

function hideCategoryStrip() {
    document.getElementById('categoryStrip').style.display = 'none';
}

function formatCategory(cat) {
    const map = {
        'CROP_DISEASE': '🦠 Disease',
        'WEATHER': '🌤️ Weather',
        'FERTILIZER': '🌱 Fertilizer',
        'MARKET_PRICE': '📈 Market',
        'GENERAL': '🌾 General'
    };
    return map[cat] || '🌾';
}

// ---- UI Utilities ----
function setLoading(state) {
    isLoading = state;
    const btn = document.getElementById('sendBtn');
    btn.disabled = state;
}

function scrollToBottom() {
    const container = document.getElementById('messagesContainer');
    container.scrollTop = container.scrollHeight;
}

function autoResize(el) {
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
}

function handleKeyDown(event) {
    // Send on Enter (without Shift)
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}
