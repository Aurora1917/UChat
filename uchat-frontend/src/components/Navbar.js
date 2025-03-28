import React, { useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../contexts/AuthContext';
import {
  AppBar, 
  Toolbar, 
  Typography, 
  Button, 
  Box, 
  Avatar
} from '@mui/material';

const Navbar = () => {
  const { currentUser, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  
  const handleLogout = async () => {
    await logout();
    navigate('/login');
    // Add window reload after navigation
    window.location.reload();
  };
  
  // Function to get the user's tag
  const getUserTag = () => {
    if (!currentUser) return null;
    
    // If the user has a tag property, use it
    if (currentUser.tag) return currentUser.tag;
    
    // If the user has a customTag property, use it
    if (currentUser.customTag) return currentUser.customTag;
    
    // If there's a username, use the first part
    if (currentUser.username) {
      const username = currentUser.username.split('#')[0];
      const discriminator = currentUser.username.split('#')[1];
      
      if (discriminator) return `#${discriminator}`;
    }
    
    return null;
  };
  
  const userTag = getUserTag();
  
  return (
    <AppBar position="static">
      <Toolbar>
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          UChat
        </Typography>
        
        {currentUser ? (
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Avatar
              sx={{ width: 32, height: 32, mr: 1 }}
            >
              {currentUser.username && currentUser.username[0].toUpperCase()}
            </Avatar>
            
            <Box sx={{ display: 'flex', alignItems: 'center', mr: 2 }}>
              <Typography variant="body1">
                {currentUser.username?.split('#')[0]}
              </Typography>
              
              {userTag && (
                <Typography 
                  variant="body2" 
                  sx={{ 
                    ml: 0.5,
                    color: 'rgba(255, 255, 255, 0.7)',
                    fontSize: '0.85rem'
                  }}
                >
                  {userTag}
                </Typography>
              )}
            </Box>
            
            <Button
              color="inherit"
              onClick={handleLogout}
              variant="outlined"
              sx={{ ml: 1 }}
            >
              Logout
            </Button>
          </Box>
        ) : (
          <Box>
            <Button
              color="inherit"
              component={Link}
              to="/login"
            >
              Login
            </Button>
            <Button
              color="inherit"
              component={Link}
              to="/register"
            >
              Register
            </Button>
          </Box>
        )}
      </Toolbar>
    </AppBar>
  );
};

export default Navbar;