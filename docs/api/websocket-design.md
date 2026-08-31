# LogiConnect Real-Time WebSocket & STOMP Architecture

## 1. Executive Summary & Design Goals

While the REST API handles standard CRUD operations, directory queries, and upload handshakes, **LogiConnect** requires real-time bidirectional communication to serve 2,000+ active logistics employees across dispatch operations, warehouse fulfillment centers, and mobile devices.

This document specifies the future WebSocket architecture using **STOMP (Simple Text Oriented Messaging Protocol) over WebSocket** (with SockJS fallback), backed by a distributed **Redis Pub/Sub & Presence Cache** for horizontal scalability.

> [!IMPORTANT]
> This document defines the future architectural contract for Phase 5. No WebSocket code or controllers are implemented in this step.

---

## 2. Infrastructure & Connection Topology

```
+------------------------------------------------------------------------------------+
|                                 Clients (Web & Mobile)                             |
+--------------------+-----------------------------+---------------------------------+
                     |                             |
                     | WSS /ws (STOMP Frame)       | WSS /ws (STOMP Frame)
                     v                             v
+------------------------------------------------------------------------------------+
|                        Layer 7 Reverse Proxy (Nginx / ALB)                         |
|                   (Sticky Sessions / WebSocket Upgrade Pass-through)               |
+--------------------+-----------------------------+---------------------------------+
                     |                             |
                     v                             v
       +----------------------------+   +----------------------------+
       |   LogiConnect Node 1       |   |   LogiConnect Node 2       |
       |  (Spring STOMP In-Memory   |   |  (Spring STOMP In-Memory   |
       |     Subscribed Clients)    |   |     Subscribed Clients)    |
       +--------------+-------------+   +--------------+-------------+
                      |                                |
                      +---------------+----------------+
                                      |
                                      v
                      +--------------------------------+
                      |      Redis 7+ Cluster          |
                      |   - Pub/Sub Channel Broker     |
                      |   - User Presence Key-Value    |
                      |   - Typing Indicator Ephemeral |
                      +--------------------------------+
```

---

## 3. Connection Handshake & Authentication

### 3.1 Handshake Endpoint
- **URL**: `wss://api.logiconnect.internal/ws`
- **Fallback URL**: `https://api.logiconnect.internal/ws` (SockJS transport for constrained firewalls)

### 3.2 Authentication via STOMP CONNECT
Web browsers cannot easily send custom HTTP headers during standard WebSocket upgrade requests. Therefore, LogiConnect enforces authentication during the STOMP `CONNECT` frame.

#### Client `CONNECT` Frame:
```stomp
CONNECT
accept-version:1.2,1.1,1.0
heart-beat:10000,10000
Authorization:Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

^@
```

#### Server `CONNECTED` Frame:
```stomp
CONNECTED
version:1.2
heart-beat:10000,10000
user-name:f0000000-0000-0000-0000-000000000002
session:sess-948271a0

^@
```

If the JWT is missing, expired, or invalid, the server immediately returns a STOMP `ERROR` frame with code `401_UNAUTHENTICATED` and terminates the underlying TCP socket.

---

## 4. Destination Topologies & Subscription Matrix

### 4.1 Server-to-Client Subscription Destinations

| Destination | Scope | Payload Type | Description |
| :--- | :--- | :--- | :--- |
| `/user/queue/messages` | User-Private | `DirectMessagePushEvent` | Inbound direct chat messages & group mentions |
| `/user/queue/notifications` | User-Private | `NotificationPushEvent` | Broadcast announcements, meeting invites, emergency alerts |
| `/topic/channels.{channelId}` | Broadcast Channel | `ChannelMessagePushEvent` | Live messages posted to organizational channel |
| `/topic/conversations.{conversationId}` | Conversation Room | `ConversationMessagePushEvent` | Live messages in direct/group conversations |
| `/topic/channels.{channelId}.typing` | Ephemeral Channel | `TypingIndicatorEvent` | Live "User is typing..." indicator in channel |
| `/topic/conversations.{convId}.typing` | Ephemeral Chat | `TypingIndicatorEvent` | Live "User is typing..." in conversation |
| `/topic/presence` | Global Presence | `PresenceChangeEvent` | Online / Away / Offline status changes |

---

## 5. Event Payload Specifications

### 5.1 Real-Time Message Event (`MessagePushEvent`)
Broadcast whenever a user sends a message in a channel or conversation.

```json
{
  "eventType": "NEW_MESSAGE",
  "data": {
    "id": "70000000-0000-0000-0000-000000000001",
    "channelId": "10000000-0000-0000-0000-000000000003",
    "conversationId": null,
    "sender": {
      "userId": "f0000000-0000-0000-0000-000000000003",
      "fullName": "Rajesh Kumar",
      "designation": "General Manager Operations",
      "profilePhotoUrl": "https://cdn.logiconnect.internal/avatars/rajesh.jpg"
    },
    "messageType": "TEXT",
    "content": "All line-haul trucks for Bangalore-Chennai corridor have departed on schedule.",
    "attachments": [],
    "createdAt": "2026-08-31T06:30:00Z"
  }
}
```

### 5.2 Real-Time Typing Indicator (`TypingIndicatorEvent`)
Sent by client when user starts typing; expires after 3 seconds on client if not refreshed.

#### Client Send Frame:
- **Destination**: `/app/chat.typing`
- **Body**:
```json
{
  "targetType": "CHANNEL",
  "targetId": "10000000-0000-0000-0000-000000000003",
  "isTyping": true
}
```

#### Server Broadcast Frame:
- **Destination**: `/topic/channels.10000000-0000-0000-0000-000000000003.typing`
```json
{
  "userId": "f0000000-0000-0000-0000-000000000003",
  "fullName": "Rajesh Kumar",
  "isTyping": true,
  "timestamp": "2026-08-31T06:30:05Z"
}
```

### 5.3 Real-Time Presence Event (`PresenceChangeEvent`)
Tracks employee online/offline state across web and mobile logistics clients.

- **Heartbeat Destination**: `/app/presence.heartbeat` (Sent by client every 30 seconds)
- **Presence State in Redis**: Key `presence:user:{userId}` with 45-second TTL.
- **Broadcast Event**:
```json
{
  "userId": "f0000000-0000-0000-0000-000000000003",
  "status": "ONLINE",
  "device": "MOBILE_ANDROID",
  "lastSeenAt": "2026-08-31T06:30:10Z"
}
```

### 5.4 Urgent Broadcast Notification (`NotificationPushEvent`)
Dispatched immediately when an emergency or high-priority announcement is published.

```json
{
  "eventType": "NEW_ANNOUNCEMENT",
  "data": {
    "notificationId": "90000000-0000-0000-0000-000000000001",
    "referenceType": "ANNOUNCEMENT",
    "referenceId": "20000000-0000-0000-0000-000000000001",
    "priority": "EMERGENCY",
    "title": "Severe Weather Alert: Heavy Rainfall at Bangalore Hub",
    "message": "Loading dock bays 4-8 temporarily closed. Mandatory acknowledgement required.",
    "requiresAcknowledgement": true,
    "createdAt": "2026-08-31T06:30:15Z"
  }
}
```

### 5.5 Read Receipt & Unread Counter Sync (`ReadReceiptEvent`)
Sent when a participant opens a conversation or acknowledges an announcement, updating unread badge counts across all active sessions of that user.

```json
{
  "eventType": "CONVERSATION_READ",
  "conversationId": "50000000-0000-0000-0000-000000000001",
  "userId": "f0000000-0000-0000-0000-000000000002",
  "lastReadMessageId": "70000000-0000-0000-0000-000000000001",
  "readAt": "2026-08-31T06:30:20Z"
}
```

---

## 6. Multi-Node Scalability & Redis Integration

When multiple Spring Boot backend instances are deployed behind the load balancer:
1. **Inbound WebSocket message on Node A**:
   - Validates user permissions.
   - Persists message to PostgreSQL.
   - Publishes JSON event payload to Redis channel `logiconnect:events:channel:{channelId}`.
2. **Redis Pub/Sub Distribution**:
   - All backend instances (Node A, Node B, Node N) subscribed to Redis receive the message.
3. **Local Client Fan-Out**:
   - Each node broadcasts the message to its own locally connected STOMP subscribers on `/topic/channels.{channelId}`.

This architecture ensures horizontal scaling across multiple application nodes with zero message loss or duplicate database writes.
