package com.example.expensetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.expensetracker.R

class RecordatorioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicamento = intent.getStringExtra("medicamento") ?: "Medicina"
        val hora = intent.getIntExtra("hora", 0)
        val minuto = intent.getIntExtra("minuto", 0)
        val notificationId = intent.getIntExtra("notificationId", 0)

        val channelId = "medicamento_recordatorio"
        val channelName = "Recordatorios de Medicamentos"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para recordar tomar medicamentos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir la app al tocar la notificación
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¡Hora de tu medicamento!")
            .setContentText("Toma: $medicamento a las %02d:%02d".format(hora, minuto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())

        // Reprogramar la alarma para el día siguiente
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val nextIntent = Intent(context, RecordatorioReceiver::class.java).apply {
            putExtra("medicamento", medicamento)
            putExtra("hora", hora)
            putExtra("minuto", minuto)
            putExtra("notificationId", notificationId)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextCalendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hora)
            set(java.util.Calendar.MINUTE, minuto)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            nextCalendar.timeInMillis,
            nextPendingIntent
        )
    }
}
