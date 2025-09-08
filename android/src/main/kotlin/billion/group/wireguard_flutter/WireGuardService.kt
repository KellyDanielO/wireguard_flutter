package billion.group.wireguard_flutter

import android.content.Intent
import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.*

import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel

class WireGuardService : VpnService() {

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
}
