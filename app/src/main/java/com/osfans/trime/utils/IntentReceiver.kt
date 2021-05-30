package com.osfans.trime.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.osfans.trime.ime.Rime
import kotlin.system.exitProcess


private val TITLE_TAG = IntentReceiver::class.java.simpleName

/** Receive Intent Broadcast Event **/
class IntentReceiver: BroadcastReceiver() {

    private val COMMAND_DEPLOY = "com.osfans.trime.deploy"
    private val COMMAND_SYNC = "com.osfans.trime.sync"

    override fun onReceive(context: Context?, intent: Intent?) {
        val command = intent?.action

        Log.d(TITLE_TAG, "Receive command = $command")
        when (command) {
            COMMAND_DEPLOY -> {
                Function.deploy(context)
                exitProcess(0)
            }
            COMMAND_SYNC -> Function.sync(context)
            Intent.ACTION_SHUTDOWN -> Rime.destroy()
            else -> {}
        }
    }

    fun registerReceiver(context: Context?) {
        context?.let {
            it.registerReceiver(this, IntentFilter(COMMAND_DEPLOY))
            it.registerReceiver(this, IntentFilter(COMMAND_SYNC))
            it.registerReceiver(this, IntentFilter(Intent.ACTION_SHUTDOWN))

        }
    }

    fun unregisterReceiver(context: Context?) { context?.unregisterReceiver(this) }

}