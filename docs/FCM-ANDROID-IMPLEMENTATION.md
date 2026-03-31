# FCM Integration for Android — Alarm-Style Scheduled Notifications

This document describes the steps to integrate Firebase Cloud Messaging (FCM) in the Checkin Android app and trigger notifications at user-configured times, similar to clock alarms.

**Backend changes:** See [FCM-BACKEND-IMPLEMENTATION.md](./FCM-BACKEND-IMPLEMENTATION.md) for Spring Boot updates.

---

## Overview

**Goal:** Send push notifications to app users at scheduled times (e.g. 9:00 AM daily) to remind them to check in, like a clock alarm.

**Approach:** Backend-driven scheduling. The Android app registers its FCM token with the backend. The backend stores the user's notification schedule and sends FCM messages at the configured times.

---

## Prerequisites

- Android Studio (latest)
- Firebase project (create at [Firebase Console](https://console.firebase.google.com))
- Backend API with FCM endpoint (see `FCM-BACKEND-IMPLEMENTATION.md`)

---

## Part 1: Firebase Setup

### 1.1 Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project or use existing
3. Add an **Android** app:
   - Package name: `com.checkin` (or your app's package)
   - Download `google-services.json` → place in `app/` module

### 1.2 Enable Cloud Messaging

1. In Firebase Console → **Build** → **Cloud Messaging**
2. Ensure Cloud Messaging is enabled
3. Note the **Server Key** (legacy) or use **Firebase Admin SDK** (recommended; backend uses service account)

### 1.3 Get FCM Credentials for Backend

1. **Project Settings** → **Service accounts**
2. **Generate new private key** → download JSON
3. Backend uses this for Firebase Admin SDK

---

## Part 2: Android Dependencies

### 2.1 Project-Level `build.gradle`

```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.0'
    }
}
```

### 2.2 App-Level `build.gradle`

```gradle
plugins {
    id 'com.google.gms.google-services'
}

dependencies {
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-messaging'
    implementation 'com.google.firebase:firebase-installations'
}
```

---

## Part 3: FCM Service & Token Handling

### 3.1 Create FirebaseMessagingService

```kotlin
// service/CheckinFirebaseMessagingService.kt
class CheckinFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to backend whenever it changes
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Checkin",
                body = notification.body ?: "Time to check in!"
            )
        }
        // Handle data payload if needed
        remoteMessage.data?.let { data ->
            val type = data["type"]
            val scheduledTime = data["scheduledAt"]
            // Optional: custom handling
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "checkin_reminders"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Check-in Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                enableLights(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    null
                )
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun sendTokenToBackend(token: String) {
        // Use your API client; requires auth token
        val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val accessToken = prefs.getString("access_token", null) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.updateDeviceToken(token) // PUT /api/user/details with fcmToken
            } catch (e: Exception) {
                Log.e("FCM", "Failed to send token to backend", e)
            }
        }
    }
}
```

### 3.2 Register Service in `AndroidManifest.xml`

```xml
<service
    android:name=".service.CheckinFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

### 3.3 Request FCM Token After Login

After successful login (or on app startup if already logged in):

```kotlin
// In LoginActivity or MainActivity after auth
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (!task.isSuccessful) {
        Log.w("FCM", "FCM token fetch failed", task.exception)
        return@addOnCompleteListener
    }
    val token = task.result
    apiService.updateDeviceToken(token) // PUT /api/user/details
}
```

---

## Part 4: Alarm-Like Notification Behavior

### 4.1 Notification Channel for Reminders

Use a high-priority channel so notifications behave like alarms:

- **Importance:** `IMPORTANCE_HIGH` or `IMPORTANCE_MAX`
- **Sound:** `TYPE_ALARM` (uses alarm sound)
- **Category:** `CATEGORY_REMINDER`
- **Full-screen intent:** Optional, for true alarm-style takeover

### 4.2 Full-Screen Intent (Optional — Strong Alarm UX)

To show a full-screen overlay like an alarm:

```kotlin
val fullScreenIntent = Intent(this, AlarmActivity::class.java)
val fullScreenPendingIntent = PendingIntent.getActivity(
    this, 0, fullScreenIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

val notification = NotificationCompat.Builder(this, channelId)
    // ... other builder calls
    .setFullScreenIntent(fullScreenPendingIntent, true)
    .build()
```

**Note:** Requires `USE_FULL_SCREEN_INTENT` permission and user grant on Android 10+.

### 4.3 Do Not Disturb / Focus Mode

- Use `NotificationChannel.setBypassDnd(true)` if you want reminders to break through Do Not Disturb (user must allow).

---

## Part 5: Sync Token with Backend

### 5.1 When to Send Token

| Event | Action |
|-------|--------|
| After login | Get FCM token, call `PUT /api/user/details` with `fcmToken` |
| `onNewToken` callback | Call `PUT /api/user/details` with new token |
| App startup (if logged in) | Optionally refresh token and sync |

### 5.2 API Call (Reuse Existing Endpoint)

```kotlin
// PUT /api/user/details
// Body: { "fcmToken": "xxx" }
api.updateDetails(UpdateDetailsRequest(fcmToken = token))
```

No new endpoints required — backend extends `UpdateAppUserRequest` with optional `fcmToken`.

---

## Part 6: Testing

### 6.1 Test Notification from Firebase Console

1. Firebase Console → **Cloud Messaging** → **Send your first message**
2. Target: your app or a test device
3. Verify notification appears with correct sound/priority

### 6.2 Test Scheduled Notifications

1. Configure a reminder time in the app (via `PUT /api/user/details` with `notificationTimes`)
2. Wait for backend to send at that time, or trigger manually from backend for testing

### 6.3 Local Testing (Without Backend)

Use `AlarmManager` + `WorkManager` to simulate: schedule a local notification for 1 minute from now to verify UX.

---

## Part 7: Checklist

- [ ] Firebase project created, Android app added, `google-services.json` in place
- [ ] FCM dependencies added, `FirebaseMessagingService` implemented
- [ ] Service registered in `AndroidManifest`
- [ ] Token requested after login and sent to backend via `PUT /details`
- [ ] `onNewToken` sends updated token to backend
- [ ] Notification channel uses alarm sound and high priority
- [ ] Full-screen intent (optional) implemented for alarm-style UX
- [ ] Background restrictions: ensure app is not overly restricted (Battery Optimisation, etc.) for reliable delivery

---

## References

- [FCM Android Setup](https://firebase.google.com/docs/cloud-messaging/android/client)
- [Notification Channels](https://developer.android.com/develop/ui/views/notifications/channels)
- [AlarmManager](https://developer.android.com/training/scheduling/alarms) (for local scheduling fallback)
