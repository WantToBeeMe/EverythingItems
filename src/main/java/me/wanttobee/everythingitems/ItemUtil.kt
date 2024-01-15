package me.wanttobee.everythingitems

import me.wanttobee.everythingitems.interactiveinventory.InteractiveInventorySystem
import me.wanttobee.everythingitems.interactiveitems.InteractiveHotBarSystem
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

object ItemUtil {
    // this is the same plugin as you provided with the initialize method. its here so everything in the
    // library knows of your plugin
    lateinit var minecraftPlugin : JavaPlugin
        private set
    var title : String? = null
        private set
    private lateinit var itemNamespaceKey: NamespacedKey
        private set

    // call this in the onEnable method.
    // it should be initialized once the plugin starts to make sure everything works as it should
    fun initialize(plugin: JavaPlugin, title: String?){
        minecraftPlugin = plugin
        this.title = title

        plugin.server.pluginManager.registerEvents(InteractiveHotBarSystem, plugin)
        plugin.server.pluginManager.registerEvents(InteractiveInventorySystem, plugin)
        itemNamespaceKey = NamespacedKey(plugin, "me_wanttobeeme_everything_items")
    }
    fun disablePlugin(){
        InteractiveHotBarSystem.disablePlugin()
        InteractiveInventorySystem.disablePlugin()
    }


    private var currentFactoryID = 0
    // simple factory method to create itemStacks more easily.
    // it also makes sure that every itemStack, (if you make it with this factory), that is unique with its own FactoryID
    // this FactoryID can be retrieved with ItemStack.getFactoryID()
    fun itemFactory(material: Material, title: String, lore: List<String>?, count: Int, enchanted : Boolean = false): ItemStack {
        val itemStack = ItemStack(material, count)
        val itemMeta = itemStack.itemMeta
        itemMeta?.setDisplayName(title)
        itemMeta?.persistentDataContainer?.set(itemNamespaceKey, PersistentDataType.INTEGER, currentFactoryID++)
        itemMeta?.lore = lore
        itemStack.itemMeta = itemMeta
        if (enchanted) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1)
            val meta = itemStack.itemMeta
            meta?.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            itemStack.itemMeta = meta
        }
        return itemStack
    }
    fun itemFactory(material: Material, title: String, lore: String, enchanted : Boolean = false): ItemStack {
        return itemFactory(material, title, listOf(lore), 1,enchanted)
    }
    fun itemFactory(material: Material, title: String,  lore: List<String>?, enchanted : Boolean = false): ItemStack {
        return itemFactory(material, title, lore, 1,enchanted)
    }
    fun itemFactory(material: Material, title: String, lore: String, count:Int, enchanted : Boolean = false): ItemStack {
        return itemFactory(material, title, listOf(lore), count,enchanted)
    }

    // items tacks that are created with the factory method provided in ItemUtil, they will contain a FactoryID.
    // This ID can be used to compare items, for example 2 stone itemsStacks may seem indistinguishable,
    // but with this method you can compare the 2 IDs and see that they are actually not the same stack
    fun ItemStack.getFactoryID() : Int?{
        return this
            .itemMeta
            ?.persistentDataContainer
            ?.get(itemNamespaceKey, PersistentDataType.INTEGER)
    }

    // for items like wool or glass this is a method which allows you to keep the same material, but as different color
    // it uses the chatColors which are all mapped to there "corresponding" block colors (minecraft is weird and it cant be a perfect match, so i did my best)
    // if it turns out that block doesn't exist (GREEN_BRICKS) then it will just not do anything and stay as normal
    fun Material.colorize(color: ChatColor): Material {
        val colorName : String= when(color){
            ChatColor.DARK_BLUE -> "BLUE"
            ChatColor.BLUE -> "BLUE"
            ChatColor.DARK_AQUA -> "CYAN"
            ChatColor.AQUA -> "LIGHT_BLUE"
            ChatColor.DARK_GREEN -> "GREEN"
            ChatColor.GREEN -> "LIME"
            ChatColor.YELLOW -> "YELLOW"
            ChatColor.GOLD -> "ORANGE"
            ChatColor.RED -> "RED"
            ChatColor.DARK_RED -> "BROWN"
            ChatColor.LIGHT_PURPLE -> "MAGENTA"
            ChatColor.DARK_PURPLE -> "PURPLE"
            ChatColor.BLACK -> "BLACK"
            ChatColor.DARK_GRAY -> "GRAY"
            ChatColor.GRAY -> "LIGHT_GRAY"
            else -> return this //if its white, just return the original material
        }
        val whiteMaterialName = this.name.removePrefix("WHITE")
        val coloredMaterialName = colorName + whiteMaterialName

        return try {
            Material.valueOf(coloredMaterialName)
        } catch (e: IllegalArgumentException) {
            this
        }
    }

    // returns the default (english) name of this item
    fun Material.getRealName(): String {
        val name = this.name.lowercase()
        var words = name.split("_")

        if (words.size == 2 && words[1] == "minecart") {
            words = listOf(words[1], "with", words[0])
        }
        else if(words.contains("template")) return "Smithing Template"
        else if(words.contains("music")) return "Music Disc"

        val formattedWords = words.map { word ->
            when (word) {
                "of", "on", "a", "with" -> word
                "tnt" -> word.uppercase()
                else -> word.capitalize()
            }
        }
        return formattedWords.joinToString(" ")
    }

    // returns a custom version of the subtitle (if it doesn't have one, it returns null)
    // for example all armour trims are just called "armor trim" but there unique name lies in there subtitle name
    // like "sentry armour trim"
    fun Material.getSubTitle() : String?{
        val name = this.name.lowercase()
        val words = name.split("_")
        // this doesn't contain the horns subtitle on purpose, if you want that, make a new one, otherwise it will mess up some games
        if(words.contains("template")){
            if(words.contains("trim"))
                return "Template: ${words.first().capitalize()} Armor Trim"
            return "Template: Netherite Upgrade"
        }
        if(words.contains("music"))
            return "disc: ${words.last()}"

        return null
    }
}
