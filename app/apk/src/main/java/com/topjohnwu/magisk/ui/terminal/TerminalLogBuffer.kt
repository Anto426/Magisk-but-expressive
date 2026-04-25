package com.topjohnwu.magisk.ui.terminal

import androidx.compose.runtime.mutableStateListOf
import com.topjohnwu.superuser.CallbackList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.Writer
import java.util.Collections

class TerminalLogBuffer {
    val lines = mutableStateListOf<String>()
    val logs: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private val lineChannel = Channel<String>(Channel.UNLIMITED)

    val console = object : CallbackList<String>() {
        override fun onAddElement(e: String?) {
            e ?: return
            logs.add(e)
            lineChannel.trySend(e)
        }
    }

    fun bind(scope: CoroutineScope) {
        scope.launch(Dispatchers.Main.immediate) {
            for (line in lineChannel) {
                lines.add(line)
            }
        }
    }

    fun writeTo(writer: Writer) {
        synchronized(logs) {
            logs.forEach {
                writer.write(it)
                writer.write("\n")
            }
        }
    }

    fun close() {
        lineChannel.close()
    }
}
