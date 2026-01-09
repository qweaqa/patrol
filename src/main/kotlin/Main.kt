package com.qweaq.patrol

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class Patrol : JavaPlugin(), CommandExecutor {

    private class PatrolSession {
        val visitedPlayers = LinkedList<UUID>()
        var lastUpdateTime = System.currentTimeMillis()
    }

    private val adminSessions = mutableMapOf<UUID, PatrolSession>()

    override fun onEnable() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        saveDefaultConfig()

        getCommand("patrol")?.setExecutor(this)
        getCommand("pmsg")?.setExecutor(this)

        logger.info("Patrol (v1.0) 已加载！")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        // --- 命令: /patrol ---
        if (label.equals("patrol", ignoreCase = true)) {

            // 重载指令
            if (args.isNotEmpty() && args[0].equals("reload", ignoreCase = true)) {
                if (!sender.hasPermission("patrol.admin")) {
                    sender.sendMessage(Component.text("你没有权限重载配置文件。", NamedTextColor.RED))
                    return true
                }
                reloadConfig()
                sender.sendMessage(Component.text("Patrol 配置文件已成功重载！", NamedTextColor.GREEN))
                logger.info("${sender.name} 重载了插件配置。")
                return true
            }

            if (sender !is Player) {
                sender.sendMessage(Component.text("控制台只能使用 /patrol reload。", NamedTextColor.RED))
                return true
            }

            if (!sender.hasPermission("patrol.use")) {
                sender.sendMessage(Component.text("无权使用。", NamedTextColor.RED))
                return true
            }

            if (sender.gameMode != GameMode.SPECTATOR) {
                sender.sendMessage(Component.text("错误：你必须处于观察者模式才能使用巡逻指令！", NamedTextColor.RED))
                sender.sendMessage(Component.text("请先输入/gmsp", NamedTextColor.GRAY))
                return true
            }

            handlePatrol(sender)
            return true
        }

        // --- 命令: /pmsg ---
        if (label.equals("pmsg", ignoreCase = true)) {
            handlePatrolMessage(sender, args)
            return true
        }

        return false
    }

    private fun handlePatrol(admin: Player) {
        val adminId = admin.uniqueId
        val currentTime = System.currentTimeMillis()
        val session = adminSessions.getOrPut(adminId) { PatrolSession() }

        val expireMinutes = config.getLong("settings.expire-minutes", 60L)

        if (currentTime - session.lastUpdateTime > expireMinutes * 60 * 1000L) {
            session.visitedPlayers.clear()
            admin.sendMessage(Component.text("巡逻列表已超时重置。", NamedTextColor.GRAY))
        }

        var candidates = Bukkit.getOnlinePlayers().filter { p ->
            p.uniqueId != adminId && !session.visitedPlayers.contains(p.uniqueId)
        }

        //  修改点 增加死锁检测与自动重置逻辑
        if (candidates.isEmpty()) {
            val allOthers = Bukkit.getOnlinePlayers().filter { it.uniqueId != adminId }

            if (allOthers.isEmpty()) {
                admin.sendMessage(Component.text("无目标可巡逻 (服务器当前没有其他玩家)。", NamedTextColor.RED))
                return
            } else {
                session.visitedPlayers.clear()
                candidates = allOthers

                admin.sendMessage(Component.text("⚠ 所有玩家均已巡逻过，自动重置名单开启新一轮循环！", NamedTextColor.AQUA))
            }
        }

        val target = candidates.random()

        admin.teleport(target)
        admin.sendMessage(
            Component.text("正在巡逻: ", NamedTextColor.GREEN)
                .append(Component.text(target.name, NamedTextColor.YELLOW))
        )

        val historySize = config.getInt("settings.history-size", 10)

        session.visitedPlayers.remove(target.uniqueId)
        session.visitedPlayers.addLast(target.uniqueId)
        if (session.visitedPlayers.size > historySize) {
            session.visitedPlayers.removeFirst()
        }
        session.lastUpdateTime = currentTime
    }

    private fun handlePatrolMessage(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("patrol.admin")) {
            sender.sendMessage(Component.text("无权使用。", NamedTextColor.RED))
            return
        }
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("用法: /pmsg <玩家>", NamedTextColor.RED))
            return
        }
        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage(Component.text("玩家不在线。", NamedTextColor.RED))
            return
        }

        val configMsg = config.getString("pmsg.message") ?: "&c查岗中..."
        val componentMsg = LegacyComponentSerializer.legacyAmpersand().deserialize(configMsg)

        val soundCount = config.getInt("pmsg.sound-count", 4)
        val soundInterval = config.getLong("pmsg.sound-interval", 10L)

        target.sendMessage(componentMsg)

        sender.sendMessage(
            Component.text("已向 ", NamedTextColor.GREEN)
                .append(Component.text(target.name, NamedTextColor.YELLOW))
                .append(Component.text(" 发送查岗提示。", NamedTextColor.GREEN))
        )

        playAlertSound(target, soundCount, soundInterval)
    }

    private fun playAlertSound(target: Player, count: Int, intervalTicks: Long) {
        object : BukkitRunnable() {
            var currentCount = 0
            override fun run() {
                if (currentCount >= count || !target.isOnline) {
                    this.cancel()
                    return
                }
                target.playSound(target.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f)
                currentCount++
            }
        }.runTaskTimer(this, 0L, intervalTicks)
    }
}