# FCM Backend Implementation — Spring Boot

This document describes the backend changes needed to support FCM push notifications at user-configured times (alarm-style). The design keeps changes **minimal** and **reuses existing endpoints** — no new public API surfaces.

**Android integration:** See [FCM-ANDROID-IMPLEMENTATION.md](./FCM-ANDROID-IMPLEMENTATION.md) for FCM client setup.

## Implementation status (this repository)

The backend items below are implemented in `com.checkin`. API paths use the servlet context path `/checkin` (e.g. `POST /checkin/api/user/register`).

| Area | Status |
|------|--------|
| Migration V10, `User`, DTOs, `AuthService`, `AppUserService`, validation (HH:mm, max 5) | Done |
| `FcmProperties`, `FcmService`, `ScheduledNotificationScheduler`, `UserRepository.findAllWithFcmTokenAndSchedule` | Done |
| Firebase Admin in `build.gradle`, `app.fcm` in `application.yml` | Done |
| OpenAPI optional fields | Done |
| `LoginRequest` optional `fcmToken` on login | Not implemented (doc recommends `PUT /details` instead) |
| `AccountDetailsResponse` / `LoginService` FCM fields | Not implemented (app uses `AppUserController` + `AppUserDetailsResponse`) |
| Unit tests for FCM flows | Not implemented |

---

## Design Principles

1. **No new endpoints** — extend `RegisterRequest`, `LoginRequest` (optional), and `UpdateAppUserRequest` with optional device fields.
2. **Single device per user (for now)** — store one FCM token per user; can be extended later for multiple devices.
3. **Schedule stored with user** — notification times live alongside user preferences.
4. **Scheduler sends FCM** — reuse the existing `@Scheduled` pattern (like `InactiveUserScheduler`).

---

## Part 1: Database Changes

### 1.1 Migration: Add Device and Schedule Columns

Create `V10__user_fcm_device.sql`:

```sql
-- FCM device token and notification schedule for push notifications (alarm-style)
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS fcm_token TEXT NULL;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS notification_times_json TEXT NULL;

COMMENT ON COLUMN app_user.fcm_token IS 'FCM device token for push notifications';
COMMENT ON COLUMN app_user.notification_times_json IS 'JSON array of daily reminder times in HH:mm (UTC), e.g. ["09:00", "18:00"]';
```

**Schema note:** `notification_times_json` stores times like `["09:00", "18:00"]` in UTC. The app sends times in the user's timezone; conversion can be done client-side or backend.

---

## Part 2: Model Changes

### 2.1 Extend `User` Entity

Add to `com.checkin.model.User`:

```java
@Column(name = "fcm_token")
private String fcmToken;

@Column(name = "notification_times_json")
private String notificationTimesJson;

// getters and setters
```

---

## Part 3: DTO Changes (Reuse Existing Endpoints)

### 3.1 Extend `RegisterRequest`

Add optional fields for device registration at sign-up:

```java
/** Optional. FCM token for push notifications. Can also be set later via PUT /details. */
@Size(max = 512)
private String fcmToken;

/** Optional. Daily reminder times in HH:mm (user's timezone), e.g. ["09:00", "18:00"]. */
private List<String> notificationTimes;
```

### 3.2 Extend `UpdateAppUserRequest` (PUT /api/user/details)

Add optional fields for device and schedule updates:

```java
/** Optional. FCM token. Pass to register or update the device for push notifications. */
@Size(max = 512)
private String fcmToken;

/** Optional. Daily reminder times in HH:mm (user timezone), e.g. ["09:00", "18:00"]. */
private List<String> notificationTimes;
```

### 3.3 Extend `AppUserDetailsResponse` / `AccountDetailsResponse`

Include in the "me" and "details" responses so the app can show and edit schedule:

```java
private String fcmToken;           // null if not set
private List<String> notificationTimes;  // e.g. ["09:00", "18:00"]
```

---

## Part 4: Service Changes

### 4.1 AuthService — Persist Device on Register

In `AuthService.register()`:

- If `req.getFcmToken() != null`, set `user.setFcmToken(req.getFcmToken())`.
- If `req.getNotificationTimes() != null`, serialize to JSON and set `user.setNotificationTimesJson(toJson(req.getNotificationTimes()))`.

### 4.2 AppUserService — Persist Device on Update

In `AppUserService.update()` (handling `UpdateAppUserRequest`):

- If `req.getFcmToken() != null`, set `user.setFcmToken(req.getFcmToken())`.
- If `req.getNotificationTimes() != null`, set `user.setNotificationTimesJson(toJson(req.getNotificationTimes()))`.
- If `req.getFcmToken() == ""`, clear: `user.setFcmToken(null)` (user opted out).

**Note:** `AppUserService` uses `UpdateAppUserRequest`; if `LoginService`/`AccountDetailsResponse` use a different DTO, extend that as well. Keep a single source of truth for `fcmToken` and `notificationTimes`.

### 4.3 LoginService — Optional Token Refresh on Login

If the Android app sends `fcmToken` in the login body (extend `LoginRequest` optionally), update the user's token on login. This is **optional**; the app can also use `PUT /details` after login. To minimize changes, **recommend using only `PUT /details`** for token updates.

---

## Part 5: FCM Sending Service

### 5.1 Add Firebase Admin SDK Dependency

`build.gradle`:

```gradle
implementation 'com.google.firebase:firebase-admin:9.2.0'
```

### 5.2 Configuration

`application.yml`:

```yaml
app:
  fcm:
    enabled: ${FCM_ENABLED:false}
    credentials-path: ${FCM_CREDENTIALS_PATH:}
    # Base64-encoded service account JSON (e.g. for ECS / secrets)
    credentials-json-base64: ${FCM_CREDENTIALS_JSON_BASE64:}
```

### 5.3 FcmService

```java
@Service
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;
    private final boolean enabled;

    public FcmService(FcmProperties props) {
        this.enabled = props.isEnabled();
        this.firebaseMessaging = enabled ? initFirebase(props) : null;
    }

    private static FirebaseMessaging initFirebase(FcmProperties props) {
        try {
            FirebaseOptions options = /* build from credentials path or base64 */;
            FirebaseApp.initializeApp(options);
            return FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.error("Failed to init Firebase", e);
            return null;
        }
    }

    public void sendCheckinReminder(String fcmToken, String title, String body) {
        if (!enabled || firebaseMessaging == null || fcmToken == null) return;
        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putData("type", "checkin_reminder")
                .putData("scheduledAt", Instant.now().toString())
                .setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                        .setChannelId("checkin_reminders")
                        .setSound("alarm")
                        .build())
                    .build())
                .build();
            firebaseMessaging.send(message);
        } catch (Exception e) {
            log.warn("FCM send failed for token", e);
            // Optionally clear invalid token in DB
        }
    }
}
```

---

## Part 6: Scheduled Notification Sender

### 6.1 Scheduler

Create `ScheduledNotificationScheduler`:

```java
@Component
public class ScheduledNotificationScheduler {

    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Scheduled(cron = "0 * * * * *")  // Every minute
    public void sendScheduledReminders() {
        Instant now = Instant.now();
        String currentTime = now.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm"));

        userRepository.findAllWithFcmTokenAndSchedule()
            .stream()
            .filter(user -> hasNotificationAt(user.getNotificationTimesJson(), currentTime))
            .forEach(user -> fcmService.sendCheckinReminder(
                user.getFcmToken(),
                "Check-in reminder",
                "Time to check in! Tap to open the app."
            ));
    }

    private boolean hasNotificationAt(String json, String currentTime) {
        if (json == null) return false;
        try {
            List<String> times = new ObjectMapper().readValue(json, new TypeReference<>() {});
            return times != null && times.contains(currentTime);
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 6.2 Repository Method

```java
// UserRepository
@Query("SELECT u FROM User u WHERE u.fcmToken IS NOT NULL AND u.notificationTimesJson IS NOT NULL AND u.notificationTimesJson != ''")
List<User> findAllWithFcmTokenAndSchedule();
```

---

## Part 7: API Summary (No New Endpoints)

| Endpoint | Change |
|----------|--------|
| `POST /api/user/register` | Request body may include `fcmToken`, `notificationTimes` (optional) |
| `PUT /api/user/details` | Request body may include `fcmToken`, `notificationTimes` (optional) |
| `GET /api/user/me` | Response includes `fcmToken`, `notificationTimes` (for display) |

All existing clients continue to work; new fields are optional.

**This app:** `server.servlet.context-path` is `/checkin`, so full paths are `POST /checkin/api/user/register`, `PUT /checkin/api/user/details`, `GET /checkin/api/user/me`.

---

## Part 8: Security & Validation

- Validate `notificationTimes` format: each element matches `HH:mm` (00:00–23:59).
- Limit number of times per user (e.g. max 5).
- Do not expose raw FCM tokens in logs.
- `fcmToken` max length 512 to avoid abuse.

---

## Part 9: Implementation Checklist

- [x] Migration `V10__user_fcm_device.sql` created and applied
- [x] `User` entity updated with `fcmToken`, `notificationTimesJson`
- [x] `RegisterRequest` extended (optional `fcmToken`, `notificationTimes`)
- [x] `UpdateAppUserRequest` extended (optional `fcmToken`, `notificationTimes`)
- [x] `AppUserDetailsResponse` extended (`GET /api/user/me`, `PUT /api/user/details`)
- [ ] `AccountDetailsResponse` extended (optional; only if you use `LoginService` account endpoints for the same app)
- [x] `AuthService.register()` persists device when provided
- [x] `AppUserService.update()` persists device and schedule
- [x] Firebase Admin SDK dependency added
- [x] `FcmProperties` config bean
- [x] `FcmService` implemented
- [x] `ScheduledNotificationScheduler` implemented
- [x] `UserRepository` method to find users due for notification
- [x] OpenAPI spec updated with new optional fields
- [ ] Unit tests for new behaviour

**Production:** set `FCM_ENABLED=true` and either `FCM_CREDENTIALS_PATH` or `FCM_CREDENTIALS_JSON_BASE64`.

---

## Part 10: Timezone Handling

**Option A (Simplest):** App sends times in UTC. Backend compares with `Instant.now()` in UTC.

**Option B:** App sends timezone (e.g. `America/New_York`) and times in local. Backend converts to UTC for scheduling.

**Option C:** Store times as stored; backend uses a fixed interpretation (e.g. all in UTC). Document that `["09:00"]` means 09:00 UTC.

Recommendation: start with **Option A**; extend to B if users need local-time reminders.

---

## References

- [Firebase Admin SDK (Java)](https://firebase.google.com/docs/admin/setup)
- [FCM HTTP v1 API](https://firebase.google.com/docs/cloud-messaging/http-server-ref)
