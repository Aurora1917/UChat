import axios from 'axios';

const API_URL = 'http://localhost:8080';

// Create axios instance with default config
// Add token to requests if available

const api = axios.create({
    baseURL: API_URL,
    headers: {
      'Content-Type': 'application/json',
    },// 🔥 Cookie'yi otomatik gönder
  });
  

  api.interceptors.response.use(
    (response) => response,
    async (error) => {
      // Original request configuration
      const originalRequest = error.config;
      
      // If error is 401 and we haven't tried to refresh yet
      if (error.response && error.response.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;
        
        try {
          // Try to clear the auth state properly
          await fetch(`${API_URL}/auth/logout`, {
            method: 'POST',
            credentials: 'include'
          });
          
          // Remove from localStorage too
          localStorage.removeItem('token');
          
          // Only redirect if not already on login page
          if (window.location.pathname !== '/login') {
            window.location.href = '/login';
          }
          
          return Promise.reject(error);
        } catch (refreshError) {
          // If logout fails, still redirect to login
          if (window.location.pathname !== '/login') {
            window.location.href = '/login';
          }
          return Promise.reject(error);
        }
      }
      
      return Promise.reject(error);
    }
  );

 export const loginUser = async (username, password) => {
    try {
      const response = await api.post('/auth/login', { username, password }, {
        // Important to include credentials for cookies
        withCredentials: true
      });
      
      // Token is handled by cookie now, but keep in localStorage for consistency
      if (response.data && response.data.token) {
        localStorage.setItem('token', response.data.token);
      }
      window.location.reload();
      return response.data;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  };

export const registerUser = async (userData) => {
  try {
    const response = await api.post('/auth/register', userData);
    return response.data;
  } catch (error) {
    console.error('Registration error:', error);
    throw error;
  }
};

export const logout = () => {
  localStorage.removeItem('token');
};

export const checkAuthStatus = async () => {
  try {
    const response = await api.get('/auth/status', {
      withCredentials: true // Important for sending cookies
    });
    console.log('User authenticated:', response.data);
    return response.data;
  } catch (error) {
    console.error('Not authenticated:', error);
    return null;
  }
};


export default api;