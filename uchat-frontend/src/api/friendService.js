import api from './authService';

export const FRIENDS_URL = '/friends'; // <-- Bunu eklediğinden emin ol!

export const sendFriendRequest = async (receiverTag) => {
  try {
    const response = await api.post(`${FRIENDS_URL}?receiverTag=${receiverTag}`, {}, { withCredentials: true });
    return response.data;
  } catch (error) {
    console.error('Error sending friend request:', error);
    throw error;
  }
};

// Bekleyen arkadaşlık isteklerini getir
export const getFriendRequests = async () => {
  try {
    const response = await api.get(`${FRIENDS_URL}/requests`, { withCredentials: true });
    return response.data;
  } catch (error) {
    console.error('Error getting friend requests:', error);
    throw error;
  }
};

// Arkadaşlık isteğini kabul et
export const acceptFriendRequest = async (requestId) => {
  try {
    const response = await api.post(`${FRIENDS_URL}/accept/${requestId}`, {}, { withCredentials: true });
    return response.data;
  } catch (error) {
    console.error('Error accepting friend request:', error);
    throw error;
  }
};

// Arkadaşlık isteğini reddet
export const rejectFriendRequest = async (requestId) => {
  try {
    const response = await api.post(`${FRIENDS_URL}/reject/${requestId}`, {}, { withCredentials: true });
    return response.data;
  } catch (error) {
    console.error('Error rejecting friend request:', error);
    throw error;
  }
};

// Kullanıcının arkadaş listesini getir
export const getFriends = async () => {
  try {
    const response = await api.get(FRIENDS_URL, { withCredentials: true });
    return response.data;
  } catch (error) {
    console.error('Error getting friends:', error);
    throw error;
  }
};

// Arkadaş silme fonksiyonu
export const deleteFriend = async (friendId) => {
  try {
    const response = await api.post(`${FRIENDS_URL}/delete?friendId=${friendId}`, {}, { withCredentials: true }); // ✅ Doğru!
    return response.data;
  } catch (error) {
    console.error('Error deleting friend:', error);
    throw error;
  }
};

