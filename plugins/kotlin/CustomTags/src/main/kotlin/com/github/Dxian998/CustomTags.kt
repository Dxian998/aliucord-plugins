package com.github.Dxian998.plugins

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.fragments.SettingsPage
import com.aliucord.views.Button
import com.aliucord.views.TextInput
import com.discord.api.commands.ApplicationCommandType
import com.lytefast.flexinput.R

@Suppress("unused")
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
            "Manage custom tags (create, delete, list)",
            listOf(
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "action",
                    "Action to perform (add, remove, list)",
                    null,
                    true,
                    listOf(
                        Utils.createCommandChoice("add", "add"),
                        Utils.createCommandChoice("remove", "remove"),
                        Utils.createCommandChoice("list", "list")
                    )
                ),
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "name",
                    "Tag name"
                ),
                Utils.createCommandOption(
                    ApplicationCommandType.STRING,
                    "message",
                    "Tag message"
                )
            )
        ) { ctx ->
            val action = ctx.getRequiredString("action")
            
            when (action) {
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
                    
                    // Save the tag
                    val tags = getTags().toMutableMap()
                    tags[name] = message
                    saveTags(tags)
                    
                    // Register the command
                    registerTagCommand(name, message)
                    
                    CommandsAPI.CommandResult("Tag `$name` created successfully!", null, false)
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
                    
                    // Unregister the command
                    commands.unregisterCommand(name)
                    
                    CommandsAPI.CommandResult("Tag `$name` removed successfully!", null, false)
                }
                "list" -> {
                    val tags = getTags()
                    
                    if (tags.isEmpty()) {
                        return@registerCommand CommandsAPI.CommandResult("No custom tags created yet!", null, false)
                    }
                    
                    val tagList = tags.keys.joinToString(", ") { "`$it`" }
                    CommandsAPI.CommandResult("**Custom Tags:** $tagList", null, false)
                }
                else -> CommandsAPI.CommandResult("Invalid action! Use: add, remove, or list", null, false)
            }
        }
    }

    private fun registerTagCommand(name: String, message: String) {
        commands.registerCommand(
            name,
            "Custom tag: ${message.take(50)}${if (message.length > 50) "..." else ""}",
            emptyList()
        ) {
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

    override fun getSettingsPage(context: Context, page: String?): SettingsPage {
        return SettingsPage("Custom Tags").apply {
            setActionBarTitle("Custom Tags")
            
            addView(TextView(context, null, 0, R.i.UiKit_Settings_Item_Header).apply {
                text = "How to Use"
            })
            
            addView(TextView(context, null, 0, R.i.UiKit_Settings_Item_SubText).apply {
                text = """
                    • Use /tag add <name> <message> to create a new tag
                    • Use /tag remove <name> to delete a tag
                    • Use /tag list to see all your tags
                    • Use /<name> to send your custom tag message
                    
                    Example: /tag add hello Hello everyone! How are you doing today?
                    Then use: /hello to send the message
                """.trimIndent()
            })
            
            addView(TextView(context, null, 0, R.i.UiKit_Settings_Item_Header).apply {
                text = "Your Tags"
            })
            
            val tags = getTags()
            if (tags.isEmpty()) {
                addView(TextView(context, null, 0, R.i.UiKit_Settings_Item_SubText).apply {
                    text = "No tags created yet. Use /tag add to create one!"
                })
            } else {
                tags.forEach { (name, message) ->
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(
                            Utils.dpToPx(16),
                            Utils.dpToPx(8),
                            Utils.dpToPx(16),
                            Utils.dpToPx(8)
                        )
                        
                        addView(TextView(context, null, 0, R.i.UiKit_Settings_Item_Label).apply {
                            text = "/$name"
                        })
                        
                        addView(TextView(context, null, 0, R.i.UiKit_Settings_Item_SubText).apply {
                            text = message
                        })
                        
                        addView(Button(context).apply {
                            text = "Delete"
                            setOnClickListener {
                                val updatedTags = getTags().toMutableMap()
                                updatedTags.remove(name)
                                saveTags(updatedTags)
                                commands.unregisterCommand(name)
                                
                                Utils.showToast("Tag deleted! Restart required to see changes in settings.")
                                reRender()
                            }
                        })
                    })
                }
            }
        }
    }
}