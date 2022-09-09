@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xszq

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.Bot
import net.mamoe.mirai.Mirai
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.source

/**
 * This validator can filter processed event for multiple bots
 */
class EventValidator {
    /** Assign single bot for each group. (Group.id, Bot.id) **/
    private val botForGroup = mutableMapOf<Long, Long>()
    /** Lock ensuring concurrency **/
    private val lock = Mutex()
    /**
     * Bot list.
     * Collecting Bot instead of their ids because there can be bots with different protocols but have the same id.
     */
    val bots = mutableListOf<Bot>()

    init {
        // Add new bot info to list
        GlobalEventChannel.subscribeAlways<BotOnlineEvent> {
            lock.withLock {
                if (bots.none { it.id == bot.id && it.configuration.protocol == bot.configuration.protocol }) {
                    bots.add(bot)
                    bot.groups.forEach {
                        checkAndAssign(it, bot)
                    }
                }
            }
        }
        // Remove if it is offline
        GlobalEventChannel.subscribeAlways<BotOfflineEvent> {
            lock.withLock {
                bots.find {
                    it.id == bot.id && it.configuration.protocol == bot.configuration.protocol
                } ?.let { entry ->
                    bots.remove(entry)
                    reAssignBot()
                }
            }
        }
        GlobalEventChannel.subscribeAlways<BotJoinGroupEvent> {
            lock.withLock {
                checkAndAssign(group, bot)
            }
        }
        GlobalEventChannel.subscribeAlways<BotLeaveEvent> {
            lock.withLock {
                reAssignBot()
            }
        }
    }

    /**
     * Check if the group need to assign a bot, and assign the specified one.
     */
    fun checkAndAssign(group: Group, bot: Bot) {
        if (!botForGroup.containsKey(group.id) || bots.none { b -> b.id == botForGroup[group.id] })
            botForGroup[group.id] = bot.id
    }

    /**
     * Reassign bot for every group. Please ensure the call is under withLock.
     */
    fun reAssignBot() {
        bots.forEach { bot ->
            bot.groups.forEach {
                checkAndAssign(it, bot)
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
     * Check if the bot is dedicated for the group in the event.
     * @param event The event to check
     */
    fun isBotDedicated(event: Event): Boolean = when (event) {
        is BotJoinGroupEvent -> true
        is BotLeaveEvent -> true
        is BotMuteEvent -> true
        is BotGroupPermissionChangeEvent -> true
        is GroupEvent -> event.bot.id == botForGroup[event.group.id]
        else -> true
    }
}

/**
 * Generate a channel which won't send duplicate events to multiple accounts.
 * @param validator The validator to use
 */
fun EventChannel<Event>.validate(validator: EventValidator): EventChannel<Event> {
    return filter { validator.notByBot(it) && validator.isBotDedicated(it) }
}