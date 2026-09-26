let stompClient = null;

function initChat(consultationId, currentUserId, currentUserName, currentUserAvatar, currentUserRole) {
    const socket = new SockJS('/ws-qaute');
    stompClient = Stomp.over(socket);
    // Tắt debug log rác trên console
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        console.log('Đã kết nối STOMP thành công vào phòng tư vấn: ' + consultationId);

        // Lắng nghe tin nhắn từ topic của phòng tư vấn này
        stompClient.subscribe('/topic/consultation.' + consultationId, function (messageOutput) {
            const message = JSON.parse(messageOutput.body);
            displayMessage(message, currentUserId);
        });

        // Cuộn xuống tin nhắn mới nhất khi vừa vào phòng
        scrollToBottom();
    }, function (error) {
        console.error('Lỗi kết nối WebSocket: ', error);
    });

    // Bắt sự kiện gửi tin nhắn
    const sendBtn = document.getElementById('btnSendMessage');
    const messageInput = document.getElementById('messageInput');
    const fileInput = document.getElementById('chatFileInput');

    if (sendBtn && messageInput) {
        sendBtn.addEventListener('click', function () {
            sendMessage(consultationId, currentUserId, currentUserName, currentUserAvatar, currentUserRole);
        });

        messageInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                sendMessage(consultationId, currentUserId, currentUserName, currentUserAvatar, currentUserRole);
            }
        });
    }

    if (fileInput) {
        fileInput.addEventListener('change', function () {
            if (fileInput.files.length > 0) {
                uploadChatFileAndSend(consultationId, currentUserId, currentUserName, currentUserAvatar, currentUserRole);
            }
        });
    }
}

function sendMessage(consultationId, currentUserId, currentUserName, currentUserAvatar, currentUserRole) {
    const messageInput = document.getElementById('messageInput');
    const content = messageInput.value.trim();

    if (content && stompClient) {
        const chatMessage = {
            consultationId: consultationId,
            senderId: currentUserId,
            senderName: currentUserName,
            senderAvatar: currentUserAvatar,
            senderRole: currentUserRole,
            content: content,
            mediaUrl: null,
            messageType: 'TEXT'
        };

        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
}

function uploadChatFileAndSend(consultationId, currentUserId, currentUserName, currentUserAvatar, currentUserRole) {
    const fileInput = document.getElementById('chatFileInput');
    const file = fileInput.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);
    formData.append('folder', 'chat_attachments');

    // Upload lên Cloudinary qua API Spring Boot
    fetch('/api/v1/upload/image', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(result => {
        if (result.success && result.data) {
            const chatMessage = {
                consultationId: consultationId,
                senderId: currentUserId,
                senderName: currentUserName,
                senderAvatar: currentUserAvatar,
                senderRole: currentUserRole,
                content: file.name,
                mediaUrl: result.data.url,
                messageType: 'IMAGE'
            };
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
            fileInput.value = '';
        } else {
            alert('Tải ảnh lên thất bại');
        }
    })
    .catch(err => {
        console.error('Lỗi upload file:', err);
    });
}

function displayMessage(message, currentUserId) {
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

    itemDiv.innerHTML = `
        <img src="${avatarUrl}" class="avatar-circle" alt="${message.senderName}">
        <div>
            <div class="small text-muted mb-1 ${isOutgoing ? 'text-end' : ''}"><strong>${message.senderName}</strong></div>
            <div class="message-bubble">
                <div>${message.content || ''}</div>
                ${mediaHtml}
                <div class="message-time">${timeStr}</div>
            </div>
        </div>
    `;

    chatContainer.appendChild(itemDiv);
    scrollToBottom();
}

function scrollToBottom() {
    const chatContainer = document.getElementById('chatMessages');
    if (chatContainer) {
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }
}
