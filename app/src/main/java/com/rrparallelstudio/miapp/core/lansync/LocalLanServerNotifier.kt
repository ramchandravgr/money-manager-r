package com.rrparallelstudio.miapp.core.lansync

/** Android foreground notification while a LAN server mode is active (implemented in :app). */
interface LocalLanServerNotifier {
    fun onLanServerStarted(mode: LocalLanServerMode)

    fun onLanServerStopped()
}
