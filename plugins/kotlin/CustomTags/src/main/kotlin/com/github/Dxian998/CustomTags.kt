package com.github.Dxian998.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.entities.CommandContext
import com.aliucord.api.CommandsAPI
import com.aliucord.Utils
import com.discord.api.commands.ApplicationCommandType

@AliucordPlugin
class CustomTags : Plugin() {
    private val tagsKey = "customTags"

    override fun start(ctx: Context) {
        // Load existing tags and register their commands
        val tags = getTags()
        tags.forEach { (name, message) ->
            registerTagCommand(name, message)
        }

        // Register the main management command
        commands.registerCommand(
            "tag",
            "Manage custom tags",
            listOf(
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "action",
                    "Action: add, remove, or list",
                    null,
                    true
                ),
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "name",
                    "Tag name",
                    null,
                    false
                ),
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "message",
                    "Tag message",
                    null,
                    false
                )
            )
        ) { ctx: CommandContext ->
            val action = ctx.getRequiredString("action")
            
            when (action.lowercase()) {
                "add" -> {
                    val name = ctx.getStringOrDefault("name", "")
                    val message = ctx.getStringOrDefault("message", "")
                    
                    if (name.isEmpty()) {
                        return@registerCommand CommandsAPI.CommandResult("Please provide a tag name!", null, false)
                    }
                    if (message.isEmpty()) {
                        return@registerCommand CommandsAPI.CommandResult("Please provide a tag message!", null, false)
                    }
                    if (name.contains(" ")) {
                        return@registerCommand CommandsAPI.CommandResult("Tag name cannot contain spaces!", null, false)
                    }
                    
                    val tags = getTags().toMutableMap()
                    tags[name] = message
                    saveTags(tags)
                    
                    registerTagCommand(name, message)
                    
                    CommandsAPI.CommandResult("Tag `$name` created! Use /$name to send it.", null, false)
                }
                "remove" -> {
                    val name = ctx.getStringOrDefault("name", "")
                    
                    if (name.isEmpty()) {
                        return@registerCommand CommandsAPI.CommandResult("Please provide a tag name to remove!", null, false)
                    }
                    
                    val tags = getTags().toMutableMap()
                    if (!tags.containsKey(name)) {
                        return@registerCommand CommandsAPI.CommandResult("Tag `$name` does not exist!", null, false)
                    }
                    
                    tags.remove(name)
                    saveTags(tags)
                    
                    commands.unregisterCommand(name)
                    
                    CommandsAPI.CommandResult("Tag `$name` removed!", null, false)
                }
                "list" -> {
                    val tags = getTags()
                    
                    if (tags.isEmpty()) {
                        return@registerCommand CommandsAPI.CommandResult("No custom tags created yet! Use /tag add <name> <message>", null, false)
                    }
                    
                    val tagList = tags.keys.joinToString(", ") { "`/$it`" }
                    CommandsAPI.CommandResult("**Your Tags:**\n$tagList", null, false)
                }
                else -> CommandsAPI.CommandResult("Invalid action! Use: add, remove, or list", null, false)
            }
        }
    }

    private fun registerTagCommand(name: String, message: String) {
        val description = if (message.length > 50) {
            message.substring(0, 50) + "..."
        } else {
            message
        }
        
        commands.registerCommand(name, description) { ctx: CommandContext ->
            CommandsAPI.CommandResult(message, null, true)
        }
    }

    private fun getTags(): Map<String, String> {
        return try {
            settings.getObject(tagsKey, emptyMap<String, String>())
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveTags(tags: Map<String, String>) {
        settings.setObject(tagsKey, tags)
    }

    override fun stop(context: Context) {
        commands.unregisterAll()
    }
}