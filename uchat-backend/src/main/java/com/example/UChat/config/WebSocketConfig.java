package com.example.UChat.config;

import com.example.UChat.repository.UserRepository;
import com.example.UChat.security.JwtRequestFilter;
import jakarta.servlet.ServletConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtRequestFilter jwtRequestFilter;
    private final UserRepository userRepository;

    public WebSocketConfig(JwtRequestFilter jwtRequestFilter, UserRepository userRepository) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.userRepository = userRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Enable SockJS fallback options with CORS for your React app
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract cookies from handshake headers
                    String cookieHeader = accessor.getFirstNativeHeader("cookie");
                    if (cookieHeader != null) {
                        // Create HttpServletRequest-like object with cookies
                        HttpServletRequest requestWithCookies = new HttpServletRequestWithCookies(cookieHeader);

                        try {
                            // Extract user ID from JWT cookie
                            Long userId = jwtRequestFilter.extractUserIdFromToken(requestWithCookies);

                            if (userId != null) {
                                // Get the user tag for this user ID

                                String userTag= userRepository.getUserTagById(userId);
                                // Create authentication object with userTag as the principal
                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(userTag, null, null);
                                SecurityContextHolder.getContext().setAuthentication(auth);
                                accessor.setUser(auth);

                                System.out.println("WebSocket authenticated for user: " + userTag + " (ID: " + userId + ")");
                            } else {
                                System.err.println("WebSocket authentication failed: Invalid or missing JWT token");
                            }
                        } catch (Exception e) {
                            System.err.println("WebSocket authentication error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("WebSocket authentication failed: No cookie header found");
                    }
                }
                return message;
            }
        });
    }

    // Helper class to mock HttpServletRequest with cookies
    private static class HttpServletRequestWithCookies implements HttpServletRequest {
        private final Cookie[] cookies;

        public HttpServletRequestWithCookies(String cookieHeader) {
            // Parse cookie header into Cookie objects
            this.cookies = parseCookieHeader(cookieHeader);
        }

        private Cookie[] parseCookieHeader(String cookieHeader) {
            if (cookieHeader == null || cookieHeader.isEmpty()) {
                return new Cookie[0];
            }

            String[] cookiePairs = cookieHeader.split(";\\s*");
            Cookie[] cookies = new Cookie[cookiePairs.length];

            for (int i = 0; i < cookiePairs.length; i++) {
                String[] nameValue = cookiePairs[i].split("=", 2);
                if (nameValue.length == 2) {
                    cookies[i] = new Cookie(nameValue[0], nameValue[1]);
                }
            }

            return cookies;
        }

        @Override
        public String getAuthType() {
            return "";
        }

        @Override
        public Cookie[] getCookies() {
            return cookies;
        }

        @Override
        public String getHeader(String name) {
            if ("Cookie".equalsIgnoreCase(name)) {
                // Reconstruct cookie header
                StringBuilder header = new StringBuilder();
                for (Cookie cookie : cookies) {
                    if (header.length() > 0) {
                        header.append("; ");
                    }
                    header.append(cookie.getName()).append("=").append(cookie.getValue());
                }
                return header.toString();
            }
            return null;
        }

        // You'll need to implement all the required methods from the HttpServletRequest interface
        // Basic implementations for required methods - stub implementations
        @Override public java.util.Enumeration<String> getHeaderNames() { return null; }
        @Override public java.util.Enumeration<String> getHeaders(String name) { return null; }
        @Override public int getIntHeader(String name) { return 0; }
        @Override public String getMethod() { return null; }
        @Override public String getPathInfo() { return null; }
        @Override public String getPathTranslated() { return null; }
        @Override public String getContextPath() { return null; }
        @Override public String getQueryString() { return null; }
        @Override public String getRemoteUser() { return null; }
        @Override public boolean isUserInRole(String role) { return false; }
        @Override public java.security.Principal getUserPrincipal() { return null; }
        @Override public String getRequestedSessionId() { return null; }
        @Override public String getRequestURI() { return null; }
        @Override public StringBuffer getRequestURL() { return null; }
        @Override public String getServletPath() { return null; }
        @Override public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
        @Override public jakarta.servlet.http.HttpSession getSession() { return null; }
        @Override public String changeSessionId() { return null; }
        @Override public boolean isRequestedSessionIdValid() { return false; }
        @Override public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override public boolean isRequestedSessionIdFromURL() { return false; }
        @Override public boolean authenticate(jakarta.servlet.http.HttpServletResponse response) { return false; }
        @Override public void login(String username, String password) { }
        @Override public void logout() { }
        @Override public java.util.Collection<jakarta.servlet.http.Part> getParts() { return null; }
        @Override public jakarta.servlet.http.Part getPart(String name) { return null; }
        @Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
        @Override public Object getAttribute(String name) { return null; }
        @Override public java.util.Enumeration<String> getAttributeNames() { return null; }
        @Override public String getCharacterEncoding() { return null; }
        @Override public void setCharacterEncoding(String env) { }
        @Override public int getContentLength() { return 0; }
        @Override public long getContentLengthLong() { return 0; }
        @Override public String getContentType() { return null; }
        @Override public jakarta.servlet.ServletInputStream getInputStream() { return null; }
        @Override public String getParameter(String name) { return null; }
        @Override public java.util.Enumeration<String> getParameterNames() { return null; }
        @Override public String[] getParameterValues(String name) { return null; }
        @Override public java.util.Map<String, String[]> getParameterMap() { return null; }
        @Override public String getProtocol() { return null; }
        @Override public String getScheme() { return null; }
        @Override public String getServerName() { return null; }
        @Override public int getServerPort() { return 0; }
        @Override public java.io.BufferedReader getReader() { return null; }
        @Override public String getRemoteAddr() { return null; }
        @Override public String getRemoteHost() { return null; }
        @Override public void setAttribute(String name, Object o) { }
        @Override public void removeAttribute(String name) { }
        @Override public java.util.Locale getLocale() { return null; }
        @Override public java.util.Enumeration<java.util.Locale> getLocales() { return null; }
        @Override public boolean isSecure() { return false; }
        @Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        @Override public int getRemotePort() { return 0; }
        @Override public String getLocalName() { return null; }
        @Override public String getLocalAddr() { return null; }
        @Override public int getLocalPort() { return 0; }
        @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync() { return null; }
        @Override public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) { return null; }
        @Override public boolean isAsyncStarted() { return false; }
        @Override public boolean isAsyncSupported() { return false; }
        @Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
        @Override public jakarta.servlet.DispatcherType getDispatcherType() { return null; }

        @Override
        public String getRequestId() {
            return "";
        }

        @Override
        public String getProtocolRequestId() {
            return "";
        }

        @Override
        public ServletConnection getServletConnection() {
            return null;
        }

        @Override public long getDateHeader(String name) { return 0; }
    }
}