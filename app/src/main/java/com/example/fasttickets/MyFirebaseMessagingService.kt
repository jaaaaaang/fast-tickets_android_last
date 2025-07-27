//package com.example.fasttickets

//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.util.Log
//import androidx.appcompat.app.AlertDialog
//import androidx.core.app.NotificationCompat
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//
//
//
//
//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        super.onMessageReceived(remoteMessage)
//
//        Log.d("FCM", "From: ${remoteMessage.from}")
//
//        // 알림 메시지가 포함된 경우 처리
//        remoteMessage.notification?.let {
//            Log.d("FCM", "Message Notification Title: ${it.title}")
//            Log.d("FCM", "Message Notification Body: ${it.body}")
//            sendNotification(it.title, it.body)
//        }
//    }
//
//
//    private fun sendNotification(title: String?, body: String?) {
//        val channelId = "default_channel"
//        val notificationId = System.currentTimeMillis().toInt()
//
//        // 알림 클릭 시 MainActivity 실행 및 데이터 전달
//        val intent = Intent(this, MainActivity::class.java).apply {
//            putExtra("title", title)
//            putExtra("body", body)
//            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            this,
//            0,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notificationBuilder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle(title ?: "알림 제목")
//            .setContentText(body ?: "알림 내용")
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(pendingIntent)
//
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        // Android 8.0 이상에서 Notification Channel 생성
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId, "Default Channel", NotificationManager.IMPORTANCE_HIGH
//            )
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        notificationManager.notify(notificationId, notificationBuilder.build())
//    }
//
//    override fun onNewToken(token: String) {
//        super.onNewToken(token)
//        Log.d("FCM", "새 FCM 토큰: $token")
//        // 서버로 새 토큰 전송
//        sendTokenToServer(token)
//    }
//
//    private fun sendTokenToServer(token: String) {
//        // 서버로 FCM 토큰 전송하는 로직 구현
//        Log.d("FCM", "서버로 토큰 전송: $token")
//    }
//}
//


// 업 올드,
package com.wm.fasttickets
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import android.content.Context
import android.app.NotificationManager
import android.os.Build
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import android.util.Log


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")
        // 알림 메시지가 포함된 경우 처리
        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Title: ${it.title}")
            Log.d("FCM", "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body)
        }
        // 메시지가 데이터 메시지인지 확인
        if (remoteMessage.data.isNotEmpty()) {
            // 데이터 메시지 처리
            val messageData = remoteMessage.data
            print(messageData);
        }
    }

    // 알림 생성 함수
    private fun sendNotification(title: String?, body: String?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "default_channel"

        // 알림을 위한 채널 설정 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 빌더
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.sm_6)  // 아이콘 설정
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)

        // 알림 보내기
        notificationManager.notify(0, notificationBuilder.build())
    }
}
