let stompClient = null;
let username = null;
let displayName = null;
let currentRoom = roomName;
let typingTimeout = null;
let connectedUsers = new Set();

const usernameForm = document.getElementById('usernameForm');
const chatForm = document.getElementById('chatForm');
const messageInput = document.getElementById('message');
const messageList = document.getElementById('messageList');
const userList = document.getElementById('userList');
const typingIndicator = document.getElementById('typingIndicator');

usernameForm.addEventListener('submit', connect);
chatForm.addEventListener('submit', sendMessage);
messageInput.addEventListener('input', handleTyping);

function connect(event) {
    event.preventDefault();
    username = document.getElementById('username').value.trim();
    displayName = document.getElementById('displayName').value.trim() || username;

    if (username) {
        document.getElementById('usernameModal').style.display = 'none';
        document.getElementById('messageArea').style.display = 'flex';
        document.getElementById('messageForm').style.display = 'block';

        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, onConnected, onError);
    }
}

function onConnected() {
    stompClient.subscribe('/topic/' + currentRoom, onMessageReceived);
    stompClient.subscribe('/topic/' + currentRoom + '/typing', onTypingReceived);
    stompClient.subscribe('/user/queue/messages', onPrivateMessage);

    stompClient.send('/app/chat.addUser/' + currentRoom,
        {},
        JSON.stringify({
            sender: username,
            senderDisplayName: displayName,
            type: 'JOIN'
        })
    );

    messageInput.focus();
}

function onError(error) {
    console.error('WebSocket Error:', error);
    showMessage('Unable to connect to chat server. Please refresh.', 'error');
}

function sendMessage(event) {
    event.preventDefault();
    const messageContent = messageInput.value.trim();

    if (messageContent && stompClient) {
        const chatMessage = {
            sender: username,
            content: messageContent,
            type: 'CHAT',
            roomName: currentRoom
        };

        stompClient.send('/app/chat.sendMessage/' + currentRoom, {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
}

function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);
    
    if (message.type === 'JOIN') {
        connectedUsers.add(message.sender);
        updateUserList();
        showMessage(message.senderDisplayName + ' joined the chat', 'event');
    } else if (message.type === 'LEAVE') {
        connectedUsers.delete(message.sender);
        updateUserList();
        showMessage(message.senderDisplayName + ' left the chat', 'event');
    } else if (message.type === 'CHAT') {
        displayMessage(message);
    }
}

function onTypingReceived(payload) {
    const message = JSON.parse(payload.body);
    if (message.sender !== username) {
        showTypingIndicator(message.senderDisplayName);
    }
}

function onPrivateMessage(payload) {
    const message = JSON.parse(payload.body);
    displayPrivateMessage(message);
}

function displayMessage(message) {
    const messageElement = document.createElement('div');
    messageElement.classList.add('message');
    
    if (message.sender === username) {
        messageElement.classList.add('own-message');
    }

    const avatar = document.createElement('div');
    avatar.classList.add('avatar');
    avatar.style.backgroundColor = message.avatarColor || '#3498db';
    avatar.textContent = message.senderDisplayName ? message.senderDisplayName.charAt(0).toUpperCase() : 'U';

    const messageBody = document.createElement('div');
    messageBody.classList.add('message-body');

    const header = document.createElement('div');
    header.classList.add('message-header');
    
    const senderName = document.createElement('strong');
    senderName.textContent = message.senderDisplayName || message.sender;
    
    const timestamp = document.createElement('span');
    timestamp.classList.add('timestamp');
    timestamp.textContent = formatTime(message.timestamp);

    header.appendChild(senderName);
    header.appendChild(timestamp);

    const content = document.createElement('p');
    content.textContent = message.content;

    messageBody.appendChild(header);
    messageBody.appendChild(content);

    messageElement.appendChild(avatar);
    messageElement.appendChild(messageBody);

    messageList.appendChild(messageElement);
    messageList.scrollTop = messageList.scrollHeight;
}

function displayPrivateMessage(message) {
    const messageElement = document.createElement('div');
    messageElement.classList.add('message', 'private-message');
    
    const content = document.createElement('p');
    content.innerHTML = `<strong>Private from ${message.senderDisplayName}:</strong> ${message.content}`;
    
    messageElement.appendChild(content);
    messageList.appendChild(messageElement);
    messageList.scrollTop = messageList.scrollHeight;
}

function showMessage(message, type) {
    const messageElement = document.createElement('div');
    messageElement.classList.add('system-message', type);
    messageElement.textContent = message;
    
    messageList.appendChild(messageElement);
    messageList.scrollTop = messageList.scrollHeight;
}

function handleTyping() {
    if (stompClient && username) {
        stompClient.send('/app/chat.typing/' + currentRoom,
            {},
            JSON.stringify({
                sender: username,
                senderDisplayName: displayName
            })
        );
    }
}

function showTypingIndicator(userName) {
    typingIndicator.textContent = userName + ' is typing...';
    typingIndicator.style.display = 'block';

    clearTimeout(typingTimeout);
    typingTimeout = setTimeout(() => {
        typingIndicator.style.display = 'none';
    }, 2000);
}

function updateUserList() {
    userList.innerHTML = '';
    connectedUsers.forEach(user => {
        const li = document.createElement('li');
        li.textContent = user;
        userList.appendChild(li);
    });
    document.getElementById('userCount').textContent = connectedUsers.size;
}

function formatTime(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
}

window.addEventListener('beforeunload', () => {
    if (stompClient && username) {
        stompClient.send('/app/chat.leave/' + currentRoom,
            {},
            JSON.stringify({
                sender: username,
                type: 'LEAVE'
            })
        );
        stompClient.disconnect();
    }
});
