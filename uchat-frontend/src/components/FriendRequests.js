// src/components/FriendRequests.js
import React, { useState, useEffect } from 'react';
import { 
  Box, Typography, Button, Dialog, DialogTitle, 
  DialogContent, TextField, DialogActions, List, 
  ListItem, ListItemText, ListItemSecondaryAction, IconButton, 
  Badge, Drawer
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { sendFriendRequest, getFriendRequests, acceptFriendRequest, rejectFriendRequest } from '../api/friendService';

const FriendRequests = ({ onFriendAdded }) => {
  const [openAddDialog, setOpenAddDialog] = useState(false);
  const [openRequestsDrawer, setOpenRequestsDrawer] = useState(false);
  const [friendTag, setFriendTag] = useState('');
  const [requests, setRequests] = useState([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Load friend requests on component mount
  useEffect(() => {
    loadFriendRequests();
  }, []);

  // Reload friend requests every minute
  useEffect(() => {
    const interval = setInterval(() => {
      loadFriendRequests();
    }, 60000);
    
    return () => clearInterval(interval);
  }, []);

  const loadFriendRequests = async () => {
    try {
      const data = await getFriendRequests();
      setRequests(data);
    } catch (error) {
      console.error('Failed to load friend requests:', error);
    }
  };

  const handleSendRequest = async () => {
    try {
      setError('');
      setSuccess('');
      await sendFriendRequest(friendTag);
      setSuccess('Friend request sent successfully!');
      setFriendTag('');
      setTimeout(() => setOpenAddDialog(false), 1500);
    } catch (error) {
      setError(error.response?.data || 'Failed to send friend request');
    }
  };

  const handleAcceptRequest = async (requestId) => {
    try {
      await acceptFriendRequest(requestId);
      // Remove the request from the list
      setRequests(requests.filter(request => request.id !== requestId));
      // Refresh the friends list in parent component
      if (onFriendAdded) onFriendAdded();
    } catch (error) {
      console.error('Failed to accept friend request:', error);
    }
  };

  const handleRejectRequest = async (requestId) => {
    try {
      await rejectFriendRequest(requestId);
      // Remove the request from the list
      setRequests(requests.filter(request => request.id !== requestId));
    } catch (error) {
      console.error('Failed to reject friend request:', error);
    }
  };

  return (
    <>
      {/* Add Friend Button */}
      <Button 
        variant="contained" 
        startIcon={<PersonAddIcon />}
        onClick={() => setOpenAddDialog(true)}
        sx={{ mb: 2 }}
      >
        Add Friend
      </Button>

      {/* Friend Requests Badge */}
      <IconButton 
        color="primary" 
        onClick={() => setOpenRequestsDrawer(true)}
        sx={{ ml: 1 }}
      >
        <Badge badgeContent={requests.length} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>

      {/* Add Friend Dialog */}
      <Dialog open={openAddDialog} onClose={() => setOpenAddDialog(false)}>
        <DialogTitle>Add Friend</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Enter your friend's tag to send them a friend request.
          </Typography>
          <TextField
            autoFocus
            margin="dense"
            label="Friend Tag"
            fullWidth
            variant="outlined"
            value={friendTag}
            onChange={(e) => setFriendTag(e.target.value)}
            placeholder="username#1234"
          />
          {error && (
            <Typography color="error" variant="body2" sx={{ mt: 1 }}>
              {error}
            </Typography>
          )}
          {success && (
            <Typography color="success.main" variant="body2" sx={{ mt: 1 }}>
              {success}
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenAddDialog(false)}>Cancel</Button>
          <Button onClick={handleSendRequest} variant="contained">Send Request</Button>
        </DialogActions>
      </Dialog>

      {/* Friend Requests Drawer */}
      <Drawer
        anchor="right"
        open={openRequestsDrawer}
        onClose={() => setOpenRequestsDrawer(false)}
        sx={{ '& .MuiDrawer-paper': { width: 320 } }}
      >
        <Box sx={{ p: 2 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Friend Requests
          </Typography>
          {requests.length === 0 ? (
            <Typography variant="body2" color="textSecondary">
              No pending friend requests
            </Typography>
          ) : (
            <List>
              {requests.map((request) => (
                <ListItem key={request.id} divider>
                  <ListItemText
                    primary={request.sender.username}
                    secondary={`${request.sender.userTag} • ${new Date(request.createdAt).toLocaleDateString()}`}
                  />
                  <ListItemSecondaryAction>
                    <IconButton 
                      edge="end" 
                      color="primary" 
                      onClick={() => handleAcceptRequest(request.id)}
                      sx={{ mr: 1 }}
                    >
                      <CheckIcon />
                    </IconButton>
                    <IconButton 
                      edge="end" 
                      color="error" 
                      onClick={() => handleRejectRequest(request.id)}
                    >
                      <CloseIcon />
                    </IconButton>
                  </ListItemSecondaryAction>
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </Drawer>
    </>
  );
};

export default FriendRequests;