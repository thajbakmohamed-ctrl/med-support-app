package com.app.med_support.security;

import com.app.med_support.model.User;
import com.app.med_support.repository.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

    @Component
    public class WebSocketAuthInterceptor implements ChannelInterceptor {

        private final JWTUtils jwtUtils;
        private final UserRepository userRepository;
        private final MyUserDetailsService userDetailsService;

        public WebSocketAuthInterceptor(JWTUtils jwtUtils, UserRepository userRepository,
                MyUserDetailsService userDetailsService) {
            this.jwtUtils = jwtUtils;
            this.userRepository = userRepository;
            this.userDetailsService = userDetailsService;
        }

       @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {

            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor == null) {
                return message;
            }

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                String authorization = accessor.getFirstNativeHeader("Authorization");

                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    throw new IllegalArgumentException("Missing WebSocket token");
                }

                String token = authorization.substring(7);

                if (!jwtUtils.validateJwtToken(token)) {
                    throw new IllegalArgumentException("Invalid WebSocket token");
                }

                String email = jwtUtils.getEmailFromJwtToken(token);
                User user = userRepository.findByEmail(email);

                if (user == null || !"ACTIVE".equals(user.getStatus())) {
                    throw new IllegalArgumentException("Inactive or unknown user");
                }

                var userDetails = userDetailsService.loadUserByUsername(email);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                accessor.setUser(authentication);
            }
           if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

               if (accessor.getUser() == null) {
                   throw new IllegalArgumentException("Unauthorized subscription");
               }

               String destination = accessor.getDestination();

               if (!"/user/queue/notifications".equals(destination)) {
                   throw new IllegalArgumentException("Subscription not allowed");
               }
           }

            return message;
        }
    }
