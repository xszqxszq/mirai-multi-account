@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xszq

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.source

/**
 * This validator can prevent events being received by multiple account for multiple times.
 * @param bufSize The size of recent signatures buffer
 */
class EventValidator(private val bufSize: Int = 128) {
    /** Recent message signatures buffer **/
    private val recentMessageSigns = mutableListOf<Pair<String, Long>>()
    /** Lock ensuring concurrency **/
    private val lock = Mutex() // TODO: Improvement needed
    /**
     * Bot list.
     * Collecting Bot instead of their ids because there can be bots with different protocols but have the same id.
     */
    private val bots = mutableListOf<Bot>()

    init {
        // Add new bot info to list
        GlobalEventChannel.subscribeAlways<BotOnlineEvent> {
            lock.withLock {
                if (bots.none { it.id == bot.id && it.configuration.protocol == bot.configuration.protocol })
                    bots.add(bot)
            }
        }
        // Remove if it is offline
        GlobalEventChannel.subscribeAlways<BotOfflineEvent> {
            lock.withLock {
                bots.removeIf {
                    it.id == bot.id && it.configuration.protocol == bot.configuration.protocol
                }
            }
        }
    }

    /**
     * Check if this event is sent by other bots.
     * @param event The event to check
     */
    fun notByBot(event: Event): Boolean {
        return event !is MessageEvent || bots.none { it.id == event.sender.id }
    }

    /**
     * Check if this event is already received from other account. If not, insert in to the list.
     * @param event The event to check
     */
    suspend fun notExistAndPush(event: Event): Boolean {
        val id = getEventSign(event)
        if (id.first == "") {
            return true
        }
        lock.withLock {
            if (recentMessageSigns.none { it.first == id.first && it.second != id.second }) {
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
    fun getEventSign(event: Event): Pair<String, Long> = when (event) {
        is MessageEvent -> Pair((if (event is GroupMessageEvent) "g" else "m") +
                "#${event.subject.id}#${event.message.source.ids.first()}", event.bot.id)
        is MemberJoinEvent -> Pair("j#${event.groupId}#${event.member.id}", event.bot.id)
        is GroupMuteAllEvent -> Pair("mu#${event.groupId}#${event.new}", event.bot.id)
        else -> Pair("", 0)
    }
}

/**
 * Generate a channel which won't send duplicate events to multiple accounts.
 * @param validator The validator to use
 */
fun EventChannel<Event>.validate(validator: EventValidator): EventChannel<Event> {
    return filter { validator.notByBot(it) && validator.notExistAndPush(it) }
}