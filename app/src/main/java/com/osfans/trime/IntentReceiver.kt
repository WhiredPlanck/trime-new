/*
 * Copyright (C) 2015-present, osfans
 * waxaca@163.com https://github.com/osfans
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.osfans.trime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.osfans.trime.Function.deploy
import com.osfans.trime.Function.sync
import kotlin.system.exitProcess

/** 接收Intent廣播事件  */
class IntentReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val command = intent.action
        Log.d(TAG, "Receive Command = $command")
        //防止为空，虽然很少,但是可能会出现
        //http://stackoverflow.com/questions/15048883/intent-getaction-is-returning-null
        if (command == null) return
        when (command) {
            COMMAND_DEPLOY -> {
                deploy(ctx)
                exitProcess(0)
            }
            COMMAND_SYNC -> sync(ctx)
            Intent.ACTION_SHUTDOWN -> Rime.destroy()
            else -> {
            }
        }
    }

    fun registerReceiver(context: Context) {
        context.registerReceiver(this, IntentFilter(COMMAND_DEPLOY))
        context.registerReceiver(this, IntentFilter(COMMAND_SYNC))
        context.registerReceiver(this, IntentFilter(Intent.ACTION_SHUTDOWN))
    }

    fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(this)
    }

    companion object {
        private const val TAG = "IntentReceiver"
        private const val COMMAND_DEPLOY = "com.osfans.trime.deploy"
        private const val COMMAND_SYNC = "com.osfans.trime.sync"
    }
}