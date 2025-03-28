import React, { useState, useEffect, useContext, useRef } from 'react';
import { AuthContext } from '../contexts/AuthContext';
import { getMessages, sendMessage, deleteMessage } from '../api/chatService';
import { deleteFriend } from '../api/friendService';
import { 
  Box, Grid, Paper, List, ListItem, ListItemText, ListItemAvatar, 
  Avatar, Typography, TextField, IconButton, Divider,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import DeleteIcon from '@mui/icons-material/Delete';
import CloseIcon from '@mui/icons-material/Close';
import PersonRemoveIcon from '@mui/icons-material/PersonRemove';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import FriendRequests from '../components/FriendRequests';

const Chat = () => {
  const { currentUser } = useContext(AuthContext);
  const [friends, setFriends] = useState([]);
  const [selectedChat, setSelectedChat] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [stompClient, setStompClient] = useState(null);
  const messagesEndRef = useRef(null);
  const [hoveredMessageId, setHoveredMessageId] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  
  // New state for confirmation dialog
  const [openConfirmDialog, setOpenConfirmDialog] = useState(false);
  const [friendToRemove, setFriendToRemove] = useState(null);

  // Load friends list
  const loadFriends = async () => {
    try {
      const response = await fetch("http://localhost:8080/friends", {
        method: "GET",
        credentials: "include", 
      });
  
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
  
      const data = await response.json();
      setFriends(data);
    } catch (error) {
      console.error("Failed to load friends:", error);
    }
  };
  
  useEffect(() => {
    loadFriends();
  }, []);
  
  // Connect to WebSocket when component mounts
  useEffect(() => {
    if (!currentUser) {
      console.log("No current user, cannot connect to WebSocket");
      return;
    }

    // Get the JWT token from localStorage
    const jwtToken = localStorage.getItem('token');
    
    const socket = new SockJS('http://localhost:8080/ws');
    const client = Stomp.over(socket);
    
    // Add the JWT token as a header for authentication
    const headers = {};
    if (jwtToken) {
      headers['cookie'] = `jwtToken=${jwtToken}`;
    }
    
    client.connect(headers, () => {
      console.log('Connected to WebSocket with authentication');
      setStompClient(client);
      
      // Subscribe to user's topic for new messages
      client.subscribe(`/topic/user/${currentUser.userTag}`, (message) => {
        const receivedMessage = JSON.parse(message.body);
        
        // Check if this message is between current user and selected chat
        if (selectedChat && 
            ((receivedMessage.senderTag === selectedChat.userTag && receivedMessage.receiverTag === currentUser.userTag) ||
             (receivedMessage.senderTag === currentUser.userTag && receivedMessage.receiverTag === selectedChat.userTag))) {
          setMessages(prev => [...prev, receivedMessage]);
        }
      });

      // Also subscribe to friend request notifications
      client.subscribe(`/topic/user/${currentUser.userTag}/friend-requests`, () => {
        // Just reload friend requests when a new one arrives
        loadFriends();
      });
    }, error => {
      console.error("WebSocket connection failed:", error);
    });
  
    return () => {
      if (client && client.connected) {
        client.disconnect();
      }
    };
  }, [currentUser]);
  
  // Scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);
  
  // Load messages when selecting a chat
  useEffect(() => {
    if (selectedChat && currentUser) {
      const loadMessages = async () => {
        try {
          // Clear existing messages first
          setMessages([]);
          // Clear any error messages when switching chats
          setErrorMessage('');
          
          console.log("Loading messages for chat with:", selectedChat.userTag);
          
          // Load messages specific to this chat
          const data = await getMessages(selectedChat.userTag);
          console.log("Received messages:", data);
          
          setMessages(data);
        } catch (error) {
          console.error('Failed to load messages:', error);
        }
      };
  
      loadMessages();
    }
  }, [selectedChat, currentUser]);

  const handleSendMessage = async () => {
    if (!newMessage.trim() || !selectedChat || !currentUser) return;
    
    try {
      if (!selectedChat.userTag) {
        console.error('Selected chat does not have a userTag property');
        return;
      }
      
      // Create message object
      const messageObj = {
        content: newMessage,
        senderTag: currentUser.userTag,
        senderId: currentUser.id,
        receiverTag: selectedChat.userTag,
        timestamp: new Date().toISOString()
      };
      
      console.log("Sending message:", messageObj);
      
      // Add a local temporary message for immediate feedback
      const tempId = `temp-${Date.now()}`;
      const tempMsg = {
        ...messageObj,
        id: tempId,
        _temporary: true // Mark as temporary
      };
      setMessages(prev => [...prev, tempMsg]);
      
      // Send via REST API first to check if users are still friends
      try {
        const sentMessage = await sendMessage(selectedChat.userTag, newMessage);
        
        // If successful, replace temporary message with real one
        setMessages(prev => prev.map(msg => 
          msg.id === tempId ? sentMessage : msg
        ));
        
        // Also send message via WebSocket if connected
        if (stompClient && stompClient.connected) {
          stompClient.send(`/app/chat/${selectedChat.userTag}`, {}, JSON.stringify(messageObj));
        }
        
        setNewMessage('');
      } catch (error) {
        // Remove temporary message
        setMessages(prev => prev.filter(msg => msg.id !== tempId));
        
        // Check if the error is due to not being friends
        if (error.response && error.response.status === 403) {
          // Show error message to user
          setErrorMessage("You can no longer send messages to this user. They may have removed you from their friends list.");
          // Reload friends list to update UI
          loadFriends();
        } else {
          console.error('Failed to send message:', error);
          setErrorMessage("Failed to send message. Please try again later.");
        }
      }
    } catch (error) {
      console.error('Failed to send message:', error);
      setErrorMessage("An unexpected error occurred. Please try again later.");
    }
  };

  // New function to initiate confirmation dialog
  const confirmRemoveFriend = (friend) => {
    setFriendToRemove(friend);
    setOpenConfirmDialog(true);
  };

  // Updated function to handle friend removal after confirmation
  const handleConfirmRemove = async () => {
    if (friendToRemove) {
      await handleRemoveFriend(friendToRemove.id);
    }
    setOpenConfirmDialog(false);
    setFriendToRemove(null);
  };

  // Original function to handle the API call
  const handleRemoveFriend = async (friendId) => {
    if (!friendId) return;
    
    try {
      await deleteFriend(friendId);
      // Reload friends list
      loadFriends();
      // If the deleted friend was the selected chat, clear it
      if (selectedChat && selectedChat.id === friendId) {
        setSelectedChat(null);
        setMessages([]);
      }
    } catch (error) {
      console.error('Failed to delete friend:', error);
      setErrorMessage("Failed to remove friend. Please try again later.");
    }
  };

  // Determine if the message was sent by the current user
  const isOwnMessage = (message) => {
    if (!currentUser) return false;
    
    // Check if the message has senderId and it matches currentUser.id
    if (message.senderId && currentUser.id) {
      return message.senderId === currentUser.id;
    }
    
    if (message.senderTag && currentUser.userTag) {
      return message.senderTag === currentUser.userTag;
    }
    
    // If no reliable identifiers, assume it's not the user's message
    return false;
  };

  // Handle message deletion - now directly deletes without showing menu
  const handleDeleteMessage = async (messageId) => {
    if (!messageId) {
      console.error('Message has no ID');
      return;
    }

    try {
      await deleteMessage(messageId);
      
      // Remove the message from the UI
      setMessages(prevMessages => 
        prevMessages.filter(msg => msg.id !== messageId)
      );
      
      console.log('Message deleted successfully');
    } catch (error) {
      console.error('Failed to delete message:', error);
      setErrorMessage("Failed to delete message. Please try again later.");
    }
  };

  return (
    <Box sx={{ height: 'calc(100vh - 64px)', display: 'flex', overflow: 'hidden' }}>
      <Grid container sx={{ height: '100%' }}>
        {/* Friends List */}
        <Grid item xs={3} sx={{ height: '100%', borderRight: '1px solid #e0e0e0' }}>
          <Paper sx={{ height: '100%', borderRadius: 0, display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ p: 2, borderBottom: '1px solid #e0e0e0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Typography variant="h6">
                Friends
              </Typography>
              <FriendRequests onFriendAdded={loadFriends} />
            </Box>
            <List sx={{ flexGrow: 1, overflow: 'auto' }}>
              {friends.length === 0 ? (
                <Box sx={{ p: 2, textAlign: 'center' }}>
                  <Typography variant="body2" color="textSecondary">
                    No friends yet. Add friends to start chatting!
                  </Typography>
                </Box>
              ) : (
                friends.map((friend) => (
                  <ListItem 
                    button 
                    key={friend.id || friend.userTag} 
                    onClick={() => setSelectedChat(friend)}
                    selected={selectedChat?.id === friend.id || selectedChat?.userTag === friend.userTag}
                  >
                    <ListItemAvatar>
                      <Avatar>{friend.username ? friend.username[0].toUpperCase() : '?'}</Avatar>
                    </ListItemAvatar>
                    <ListItemText 
                      primary={friend.username} 
                      secondary={friend.lastMessage || 'No messages yet'} 
                    />
                  </ListItem>
                ))
              )}
            </List>
          </Paper>
        </Grid>
        
        {/* Chat Area */}
        <Grid item xs={9} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
          {selectedChat ? (
            <>
              {/* Chat Header */}
              <Box sx={{ p: 2, borderBottom: '1px solid #e0e0e0', flexShrink: 0, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Box>
                  <Typography variant="h6">
                    {selectedChat.username}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    {selectedChat.userTag}
                  </Typography>
                </Box>
                <IconButton 
                  color="error" 
                  size="small" 
                  onClick={() => confirmRemoveFriend(selectedChat)}
                  title="Remove friend"
                >
                  <PersonRemoveIcon fontSize="small" />
                </IconButton>
              </Box>
              
              {/* Error Message */}
              {errorMessage && (
                <Box sx={{ 
                  p: 2, 
                  bgcolor: '#ffebee', 
                  color: '#c62828', 
                  borderRadius: 1,
                  mx: 2,
                  mt: 1,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}>
                  <Typography variant="body2">{errorMessage}</Typography>
                  <IconButton 
                    size="small" 
                    onClick={() => setErrorMessage('')}
                  >
                    <CloseIcon fontSize="small" />
                  </IconButton>
                </Box>
              )}
              
              {/* Messages */}
              <Box sx={{ 
                flexGrow: 1,
                p: 2, 
                overflow: 'auto',
                display: 'flex',
                flexDirection: 'column'
              }}>
                {messages.length === 0 ? (
                  <Box sx={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'center',
                    height: '100%'
                  }}>
                    <Typography variant="body2" color="textSecondary">
                      No messages yet. Say hello!
                    </Typography>
                  </Box>
                ) : (
                  messages.map((message, index) => {
                    const own = isOwnMessage(message);
                    const messageId = message.id || `msg-${index}-${message.timestamp}`;
                    const isHovered = hoveredMessageId === messageId;
                    
                    return (
                      <Box 
                        key={messageId}
                        sx={{
                          maxWidth: '70%',
                          mb: 1,
                          p: 1,
                          pl: 2,
                          pr: 2,
                          borderRadius: 2,
                          bgcolor: own ? '#1976d2' : '#f5f5f5',
                          color: own ? 'white' : 'inherit',
                          alignSelf: own ? 'flex-end' : 'flex-start',
                          position: 'relative',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between'
                        }}
                        onMouseEnter={() => setHoveredMessageId(messageId)}
                        onMouseLeave={() => setHoveredMessageId(null)}
                      >
                        <Box sx={{ flexGrow: 1 }}>
                          <Typography variant="body1">{message.content}</Typography>
                          <Typography variant="caption" sx={{ display: 'block', mt: 0.5, opacity: 0.7 }}>
                            {message.timestamp ? new Date(message.timestamp).toLocaleTimeString() : 'Sending...'}
                          </Typography>
                        </Box>
                        
                        {/* Only show delete option for own messages with an ID - now deletes directly */}
                        {own && message.id && isHovered && !message._temporary && (
                          <IconButton 
                            size="small" 
                            onClick={() => handleDeleteMessage(message.id)}
                            sx={{ 
                              ml: 1,
                              color: own ? 'white' : 'gray',
                              padding: '2px'
                            }}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        )}
                      </Box>
                    );
                  })
                )}
                <div ref={messagesEndRef} />
              </Box>
              
              {/* Message Input */}
              <Box sx={{ p: 2, borderTop: '1px solid #e0e0e0', display: 'flex', flexShrink: 0 }}>
                <TextField
                  fullWidth
                  placeholder="Type a message"
                  variant="outlined"
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                  disabled={Boolean(errorMessage)}
                />
                <IconButton 
                  color="primary" 
                  sx={{ ml: 1 }} 
                  onClick={handleSendMessage}
                  disabled={Boolean(errorMessage)}
                >
                  <SendIcon />
                </IconButton>
              </Box>
            </>
          ) : (
            <Box 
              sx={{ 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center',
                height: '100%',
                flexDirection: 'column'
              }}
            >
              <Typography variant="h6" color="textSecondary" sx={{ mb: 2 }}>
                Select a chat to start messaging
              </Typography>
              <Typography variant="body2" color="textSecondary">
                Or add new friends using the "Add Friend" button
              </Typography>
            </Box>
          )}
        </Grid>
      </Grid>

      {/* Confirmation Dialog for Friend Removal */}
      <Dialog
        open={openConfirmDialog}
        onClose={() => setOpenConfirmDialog(false)}
      >
        <DialogTitle>Remove Friend</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to remove {friendToRemove?.username} from your friends list? 
            You won't be able to send or receive messages from them anymore.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenConfirmDialog(false)}>Cancel</Button>
          <Button onClick={handleConfirmRemove} color="error" variant="contained">
            Remove
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Chat;