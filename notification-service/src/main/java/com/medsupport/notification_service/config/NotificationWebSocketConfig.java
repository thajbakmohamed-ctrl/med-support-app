package com.medsupport.notification_service.config;

import com.medsupport.notification_service.controller.NotificationController.InboxItem;
import com.medsupport.notification_service.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocket
public class NotificationWebSocketConfig extends TextWebSocketHandler implements WebSocketConfigurer {
    private final NotificationRepository repository;
    private final RestClient identity;
    private final String origin;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private static final class Client {
        final WebSocketSession session;
        final long connected = System.currentTimeMillis();
        String bearer;
        String email;
        String lastSnapshot;
        Client(WebSocketSession session) { this.session = session; }
    }
    public record Account(String email, String status, boolean emailVerified) {}

    public NotificationWebSocketConfig(NotificationRepository repository,
            @Value("${services.identity.url}") String identityUrl,
            @Value("${app.frontend-origin:http://localhost:5500}") String origin) {
        this.repository = repository;
        this.origin = origin;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3)); factory.setReadTimeout(Duration.ofSeconds(5));
        this.identity = RestClient.builder().requestFactory(factory).baseUrl(identityUrl).build();
    }

    public String verifyEmail(String bearer) {
        if (bearer == null || !bearer.startsWith("Bearer ") || bearer.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required");
        final Account account;
        try { account = identity.get().uri("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer)
                .retrieve().body(Account.class); }
        catch (org.springframework.web.client.RestClientResponseException e) {
            throw new ResponseStatusException(e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403
                    ? HttpStatus.UNAUTHORIZED : HttpStatus.SERVICE_UNAVAILABLE, "Identity verification unavailable");
        } catch (org.springframework.web.client.RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Identity verification unavailable");
        }
        if (account == null || account.email() == null || !"ACTIVE".equals(account.status()) || !account.emailVerified())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Active verified account required");
        return account.email();
    }

    public Page<InboxItem> inbox(String email, int page) {
        return repository.findByRecipientEmailOrderByCreatedAtDescIdDesc(email, PageRequest.of(page, 20))
                .map(n -> new InboxItem(n.getId(), n.getNotificationType(), n.getTitle(), n.getMessage(), n.getBookingId(), n.getCreatedAt()));
    }

    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(this, "/ws/notifications").setAllowedOrigins(origin);
    }
    @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!origin.equals(session.getHandshakeHeaders().getOrigin()) || clients.size() >= 100) {
            session.close(CloseStatus.POLICY_VIOLATION); return;
        }
        session.setTextMessageSizeLimit(8192);
        clients.put(session.getId(), new Client(session));
    }
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Client client = clients.get(session.getId());
        if (client == null) return;
        synchronized (client) {
            if (client.bearer != null) { session.close(CloseStatus.POLICY_VIOLATION); return; }
            try {
                String token = json.readTree(message.getPayload()).path("token").asString();
                String email = verifyEmail("Bearer " + token);
                client.bearer = "Bearer " + token; client.email = email;
                sendSnapshot(client);
            } catch (Exception e) { session.close(CloseStatus.POLICY_VIOLATION); }
        }
    }
    private void sendSnapshot(Client client) throws Exception {
        String snapshot = json.writeValueAsString(inbox(client.email, 0));
        if (!snapshot.equals(client.lastSnapshot)) {
            client.session.sendMessage(new TextMessage(snapshot)); client.lastSnapshot = snapshot;
        }
    }
    @Scheduled(fixedDelay = 3000)
    public void refreshConnectedInboxes() {
        for (Client client : clients.values()) synchronized (client) {
            try {
                if (!client.session.isOpen()) { clients.remove(client.session.getId()); continue; }
                if (client.bearer == null) {
                    if (System.currentTimeMillis() - client.connected > 10000) client.session.close(CloseStatus.POLICY_VIOLATION);
                    continue;
                }
                // Revalidation closes expired tokens and deactivated accounts; no client-selected recipient.
                if (!client.email.equals(verifyEmail(client.bearer))) { client.session.close(CloseStatus.POLICY_VIOLATION); continue; }
                sendSnapshot(client);
            } catch (Exception e) {
                try { client.session.close(CloseStatus.POLICY_VIOLATION); } catch (Exception ignored) { }
            }
        }
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { clients.remove(session.getId()); }
}
