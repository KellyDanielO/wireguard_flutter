package billion.group.wireguard_flutter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel

class WireGuardService : VpnService() {

    private val CHANNEL_ID = "wireguard_vpn_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VPN Active")
            .setContentText("WireGuard tunnel is running")
            // use a generic system icon that always exists
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            // mark it as persistent
            .setOngoing(true)
            .build()

        // Keeps VPN alive in the background
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val backend = GoBackend(this@WireGuardService)
                val tunnels = backend.runningTunnelNames
                for (name in tunnels) {
                    backend.setState(WireGuardTunnel(name), Tunnel.State.DOWN, null)
                }
                Log.i("WireGuardService", "Stopped all tunnels on task removal")
            } catch (e: Exception) {
                Log.e("WireGuardService", "Failed to stop tunnels: ${e.message}")
            }
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tunneldeck VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Keeps WireGuard VPN alive in background"
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
