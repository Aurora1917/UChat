import React, { createContext, useState, useEffect } from 'react';
import { loginUser, registerUser, checkAuthStatus } from '../api/authService';
import axios from 'axios';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Check if user is already logged in on page load
  useEffect(() => {
    const checkLoggedIn = async () => {
      try {
        const user = await checkAuthStatus();
        
        if (user) {
          setCurrentUser(user);
          setIsAuthenticated(true);
          console.log("User authenticated from session:", user);
        } else {
          // If no user but not on login page, redirect
          setCurrentUser(null);
          setIsAuthenticated(false);
          
          if (window.location.pathname !== '/login' && 
              window.location.pathname !== '/register') {
            window.location.href = '/login';
          }
        }
      } catch (error) {
        console.error("Not authenticated:", error);
        setCurrentUser(null);
        setIsAuthenticated(false);
        
        // Only redirect if not already on login/register page
        if (window.location.pathname !== '/login' && 
            window.location.pathname !== '/register') {
          window.location.href = '/login';
        }
      } finally {
        setLoading(false);
      }
    };
  
    checkLoggedIn();
  }, []);

  // Login function
  const login = async (username, password) => {
    try {
      setLoading(true);
      const response = await loginUser(username, password);
      
      console.log("Login response:", response); // Debug log
      
      // With HTTP-only cookies, the token is already stored in the cookie by the server
      // We just need to update our application state
      setCurrentUser(response.user);
      setIsAuthenticated(true);
      
      return { success: true };
    } catch (error) {
      console.error("Login error:", error);
      return {
        success: false,
        message: error.response?.data?.message || 'Login failed'
      };
    } finally {
      setLoading(false);
    }
  };

  // Register function
  const register = async (userData) => {
    try {
      setLoading(true);
      const response = await registerUser(userData);
      
      return { success: true };
    } catch (error) {
      return {
        success: false,
        message: error.response?.data?.message || 'Registration failed'
      };
    } finally {
      setLoading(false);
    }
  };

  // Logout function
  const logout = async () => {
    try {
      await axios.post('http://localhost:8080/auth/logout', {}, {
        withCredentials: true // Needed to send cookies
      });
      
      // Clear any stored tokens
      localStorage.removeItem('token');
      
      // Update state
      setCurrentUser(null);
      
      // Redirect to login page
      window.location.href = '/login';
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };
  // The value that will be available through the context
  const authContextValue = {
    currentUser,
    isAuthenticated,
    loading,
    login,
    register,
    logout
  };

  return (
    <AuthContext.Provider value={authContextValue}>
      {children}
    </AuthContext.Provider>
  );
};