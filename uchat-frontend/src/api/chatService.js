import api from './authService';
import { FRIENDS_URL } from './friendService';
// Get friends list
export const getFriends = async () => {
  try {
    const response = await api.get('/friends');
    return response.data;
  } catch (error) {
    console.error('Error fetching friends:', error);
    throw error;
  }
};

// Get messages for a specific chat
export const getMessages = async (receiverTag) => {
  try {
    console.log("Requesting chat history for receiver tag:", receiverTag);
    
    const response = await api.get(`/messages/history/${receiverTag}`, {
      withCredentials: true // Ensures cookies are sent with the request
    });
    
    return response.data;
  } catch (error) {
    console.error('Error fetching messages:', error);
    throw error;
  }
};

export const sendMessage = async (receiverTag, content) => {
  try {
    console.log(`Sending message to ${receiverTag}: ${content}`);
    const response = await api.post('/messages/send', {
      receiverTag: receiverTag,
      content: content
    }, {
      withCredentials: true  // Add this to ensure cookies/auth are sent
    });
    console.log('Message sent successfully:', response.data);
    return response.data;
  } catch (error) {
    console.error('Error sending message:', error);
    console.error('Error details:', error.response?.data || error.message);
    throw error;
  }
};

export const deleteMessage = async (messageId) => {
  try {
    // Use POST as required by the backend
    const response = await api.post(`/messages/delete/${messageId}`, {}, {
      withCredentials: true // Ensure cookies are sent for authentication
    });
    return response.data;
  } catch (error) {
    console.error("Error deleting message:", error);
    throw error;
  }
};

export const deleteFriend = async (friendId) => {
  try {
    const response = await api.post(`${FRIENDS_URL}/delete?friendId=${friendId}`);
    return response.data;
  } catch (error) {
    console.error('Error deleting friend:', error);
    throw error;
  }
};