/* ====================================================================
   QAUTE CHATBOX MESSENGER SCRIPT (WEBSOCKET & STOMP REALTIME)
   ==================================================================== */

let stompClient = null;
let currentSubscription = null;
let activeConsultationId = null;
let currentChatUser = {
    id: null,
    name: '',
    avatar: '',
    role: ''
};

/**
 * Khởi tạo kết nối STOMP và thiết lập phòng chat ban đầu
 */
function initChat(consultationId, currentUserId, currentUserName, currentUserAvatar, currentUserRole) {
    currentChatUser.id = currentUserId;
    currentChatUser.name = currentUserName;
    currentChatUser.avatar = currentUserAvatar;
    currentChatUser.role = currentUserRole;
    activeConsultationId = consultationId;

    const socket = new SockJS('/ws-qaute');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Tắt log rác trên console

    stompClient.connect({}, function (frame) {
        console.log('STOMP Connected. User:', currentUserName, 'Role:', currentUserRole);

        if (activeConsultationId) {
            subscribeToConsultation(activeConsultationId);
        }

        scrollToBottom();
    }, function (error) {
        console.error('Lỗi kết nối WebSocket STOMP: ', error);
    });

    bindChatControls();
    bindSearchFilter();
}

/**
 * Đăng ký lắng nghe tin nhắn theo phòng tư vấn
 */
function subscribeToConsultation(consultationId) {
    if (!stompClient || !stompClient.connected) return;

    if (currentSubscription) {
        currentSubscription.unsubscribe();
    }

    currentSubscription = stompClient.subscribe('/topic/consultation.' + consultationId, function (messageOutput) {
        const message = JSON.parse(messageOutput.body);
        displayMessage(message, currentChatUser.id);
    });

    activeConsultationId = consultationId;
}

/**
 * Chuyển đổi cuộc trò chuyện khi click vào thẻ danh sách bên trái
 */
function switchConversation(consultationId, partnerName, partnerAvatar, title, statusText, statusClass, roleDisplayName) {
    if (activeConsultationId === consultationId) return;

    // Cập nhật giao diện header
    const partnerNameEl = document.getElementById('chatPartnerName');
    if (partnerNameEl) partnerNameEl.textContent = partnerName;

    const partnerAvatarEl = document.getElementById('chatPartnerAvatar');
    if (partnerAvatarEl) partnerAvatarEl.src = partnerAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150';

    const titleEl = document.getElementById('chatTitle');
    if (titleEl) titleEl.textContent = title;

    const roleBadgeEl = document.getElementById('chatPartnerRoleBadge');
    if (roleBadgeEl && roleDisplayName) {
        roleBadgeEl.textContent = roleDisplayName;
    }

    // Đánh dấu active trong danh sách
    document.querySelectorAll('.conversation-card').forEach(card => card.classList.remove('active'));
    const activeCard = document.querySelector(`.conversation-card[data-id="${consultationId}"]`);
    if (activeCard) {
        activeCard.classList.add('active');
        // Xóa badge tin nhắn chưa đọc nếu có
        const unreadDot = activeCard.querySelector('.badge-unread');
        if (unreadDot) unreadDot.remove();
    }

    // Chuyển STOMP topic
    subscribeToConsultation(consultationId);

    // Xóa tin nhắn cũ và tải lịch sử qua REST API
    const messagesContainer = document.getElementById('chatMessages');
    if (messagesContainer) {
        messagesContainer.innerHTML = `
            <div class="text-center py-4 text-muted">
                <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
                Đang tải tin nhắn...
            </div>
        `;

        fetch(`/api/v1/chat/${consultationId}/messages`)
            .then(res => res.json())
            .then(data => {
                messagesContainer.innerHTML = '';
                if (data.success && data.data) {
                    data.data.forEach(msg => {
                        displayMessage(msg, currentChatUser.id);
                    });
                }
                scrollToBottom();
            })
            .catch(err => {
                console.error('Lỗi tải tin nhắn:', err);
                messagesContainer.innerHTML = '<div class="text-center py-4 text-danger">Không thể tải tin nhắn. Vui lòng thử lại.</div>';
            });
    }

    // Đánh dấu đã đọc qua API
    fetch(`/api/v1/chat/${consultationId}/read`, { method: 'POST' }).catch(() => {});
}

/**
 * Gắn sự kiện cho các nút điều khiển gửi tin nhắn
 */
function bindChatControls() {
    const sendBtn = document.getElementById('btnSendMessage');
    const messageInput = document.getElementById('messageInput');
    const fileInput = document.getElementById('chatFileInput');

    if (sendBtn && messageInput) {
        sendBtn.addEventListener('click', function () {
            sendCurrentMessage();
        });

        messageInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                sendCurrentMessage();
            }
        });
    }

    if (fileInput) {
        fileInput.addEventListener('change', function () {
            if (fileInput.files.length > 0) {
                uploadChatFileAndSend();
            }
        });
    }
}

/**
 * Gửi tin nhắn văn bản
 */
function sendCurrentMessage() {
    const messageInput = document.getElementById('messageInput');
    if (!messageInput) return;

    const content = messageInput.value.trim();
    if (content && stompClient && activeConsultationId) {
        const chatMessage = {
            consultationId: activeConsultationId,
            senderId: currentChatUser.id,
            senderName: currentChatUser.name,
            senderAvatar: currentChatUser.avatar,
            senderRole: currentChatUser.role,
            content: content,
            mediaUrl: null,
            messageType: 'TEXT'
        };

        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
        messageInput.focus();
    }
}

/**
 * Tải ảnh và gửi tin nhắn hình ảnh
 */
function uploadChatFileAndSend() {
    const fileInput = document.getElementById('chatFileInput');
    if (!fileInput || !fileInput.files[0] || !activeConsultationId) return;

    const file = fileInput.files[0];
    const maxSizeBytes = 25 * 1024 * 1024; // 25MB
    if (file.size > maxSizeBytes) {
        alert('Dung lượng tệp/ảnh vượt quá 25MB! Vui lòng chọn tệp nhỏ hơn.');
        fileInput.value = '';
        return;
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('folder', 'chat_attachments');

    // Hiển thị trạng thái đang tải
    const sendBtn = document.getElementById('btnSendMessage');
    if (sendBtn) sendBtn.disabled = true;

    fetch('/api/v1/upload/image', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(result => {
        if (result.success && result.data) {
            const chatMessage = {
                consultationId: activeConsultationId,
                senderId: currentChatUser.id,
                senderName: currentChatUser.name,
                senderAvatar: currentChatUser.avatar,
                senderRole: currentChatUser.role,
                content: file.name,
                mediaUrl: result.data.url,
                messageType: 'IMAGE'
            };
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
            fileInput.value = '';
        } else {
            alert('Tải ảnh lên thất bại!');
        }
    })
    .catch(err => {
        console.error('Lỗi upload file:', err);
        alert('Có lỗi xảy ra khi tải ảnh lên.');
    })
    .finally(() => {
        if (sendBtn) sendBtn.disabled = false;
    });
}

/**
 * Hiển thị một tin nhắn vào khung chat
 */
function displayMessage(message, currentUserId) {
    hideTypingIndicator();
    const chatContainer = document.getElementById('chatMessages');
    if (!chatContainer) return;

    const isOutgoing = (message.senderId == currentUserId);
    const itemDiv = document.createElement('div');
    itemDiv.className = 'message-item ' + (isOutgoing ? 'outgoing' : 'incoming');

    let mediaHtml = '';
    if (message.mediaUrl) {
        mediaHtml = `<div class="mt-2"><a href="${message.mediaUrl}" target="_blank"><img src="${message.mediaUrl}" class="message-image" alt="Đính kèm"></a></div>`;
    }

    const timeStr = new Date(message.sentAt || new Date()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const avatarUrl = message.senderAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150';

    // Tạo Role Badge
    let roleBadgeHtml = '';
    if (message.senderRole === 'ROLE_ADMIN') {
        roleBadgeHtml = `<span class="badge-role-admin ms-1"><i class="bi bi-shield-lock-fill me-1"></i>Admin</span>`;
    } else if (message.senderRole === 'ROLE_LECTURER') {
        roleBadgeHtml = `<span class="badge-role-lecturer ms-1"><i class="bi bi-mortarboard-fill me-1"></i>Giảng viên</span>`;
    } else if (message.senderRole === 'ROLE_STUDENT') {
        roleBadgeHtml = `<span class="badge-role-student ms-1"><i class="bi bi-person-fill me-1"></i>Sinh viên</span>`;
    }

    itemDiv.innerHTML = `
        <img src="${avatarUrl}" class="avatar-circle" alt="${message.senderName || 'Avatar'}">
        <div>
            <div class="small text-muted mb-1 ${isOutgoing ? 'text-end' : ''}">
                <strong>${message.senderName || 'Người dùng'}</strong>
                ${roleBadgeHtml}
            </div>
            <div class="message-bubble">
                <div>${escapeHtml(message.content || '')}</div>
                ${mediaHtml}
                <div class="message-time">${timeStr}</div>
            </div>
        </div>
    `;

    chatContainer.appendChild(itemDiv);
    scrollToBottom();
}

/**
 * Tự động cuộn xuống cuối khung chat
 */
function scrollToBottom() {
    const chatContainer = document.getElementById('chatMessages');
    if (chatContainer) {
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }
}

/**
 * Tìm kiếm và lọc danh sách cuộc trò chuyện theo thời gian thực
 */
function bindSearchFilter() {
    const searchInput = document.getElementById('chatSearchInput');
    if (!searchInput) return;

    searchInput.addEventListener('input', function (e) {
        const query = e.target.value.toLowerCase().trim();
        const cards = document.querySelectorAll('.conversation-card');

        cards.forEach(card => {
            const name = (card.getAttribute('data-name') || '').toLowerCase();
            const title = (card.getAttribute('data-title') || '').toLowerCase();
            const code = (card.getAttribute('data-code') || '').toLowerCase();

            if (name.includes(query) || title.includes(query) || code.includes(query)) {
                card.style.display = 'flex';
            } else {
                card.style.display = 'none';
            }
        });
    });
}

/**
 * Thoát các ký tự HTML nguy hiểm để chống XSS
 */
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

/**
 * Hiển thị chỉ báo đối phương đang nhập tin nhắn
 */
function showTypingIndicator(senderName) {
    hideTypingIndicator();
    const chatContainer = document.getElementById('chatMessages');
    if (!chatContainer) return;

    const typingDiv = document.createElement('div');
    typingDiv.id = 'chatTypingIndicator';
    typingDiv.className = 'typing-indicator-wrapper';
    typingDiv.innerHTML = `
        <div class="typing-bubble">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
        </div>
        <small class="text-muted fst-italic ms-2">${escapeHtml(senderName || 'Đối phương')} đang soạn tin...</small>
    `;
    chatContainer.appendChild(typingDiv);
    scrollToBottom();
}

/**
 * Ẩn chỉ báo đang nhập
 */
function hideTypingIndicator() {
    const indicator = document.getElementById('chatTypingIndicator');
    if (indicator) {
        indicator.remove();
    }
}

