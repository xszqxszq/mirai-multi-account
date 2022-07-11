@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xszq

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupMuteAllEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.source
import java.util.*

/**
 * This validator can prevent events being received by multiple account for multiple times.
 * @param bufSize The size of recent signatures buffer
 */
class EventValidator(private val bufSize: Int = 128) {
    private val recentMessageSigns: MutableList<String> = Collections.synchronizedList(mutableListOf<String>())
    private val lock = Mutex() // TODO: Might reduce throughput, improvement needed

    /**
     * Check if this event is already received from other account. If not, insert in to the list.
     * @param event The event to check
     */
    suspend fun notExistAndPush(event: Event): Boolean {
        val id = getEventSign(event)
        if (id == "") {
            return true
        }
        lock.withLock {
            if (!recentMessageSigns.contains(id)) {
                recentMessageSigns.add(id)
                if (recentMessageSigns.size > bufSize)
                    recentMessageSigns.removeFirst()
                return true
            }
        }
        return false
    }

    /**
     * Generate signature for an event.
     * @param event Target event
     */
    fun getEventSign(event: Event): String = when (event) {
        is MessageEvent -> (if (event is GroupMessageEvent) "g" else "m") +
                "#${event.subject.id}#${event.message.source.ids.first()}"
        is MemberJoinEvent -> "j#${event.groupId}#${event.member.id}"
        is GroupMuteAllEvent -> "mu#${event.groupId}#${event.new}"
        else -> ""
    }
}

fun EventChannel<Event>.validate(validator: EventValidator): EventChannel<Event> {
    return filter { validator.notExistAndPush(it) }
}