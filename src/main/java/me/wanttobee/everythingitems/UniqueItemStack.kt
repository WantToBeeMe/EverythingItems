package me.wanttobee.everythingitems

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

// a UniqueItemStack is like any other item stack
// the only cool difference is that this is way easier to make (things like enchanting, lore an all that kind of stuff)
// and it also has a build in uniqueness system, where you can also compare item stacks with each other
// and for example check if an item stack is the same as some other item stack
// this is as specially cool because you can modify the items tack all you won't, changing the name, the material and all,
// but it will always stay the same items tack (unless you change the namespace key)
class UniqueItemStack(material: Material, title: String, lore: List<String>?, count: Int, enchanted : Boolean = false) : ItemStack(material, count) {
    constructor(material: Material, title: String, lore: String, enchanted : Boolean = false) : this(material, title, listOf(lore), 1,enchanted);
    constructor(material: Material, title: String,  lore: List<String>?, enchanted : Boolean = false) : this(material, title, lore, 1,enchanted);
    constructor(material: Material, title: String, lore: String, count:Int, enchanted : Boolean = false) : this(material, title, listOf(lore), count,enchanted)

    companion object{
        // this factory ID will be increased everytime a UniqueItemStack has been made to ensure that every stack is unique
        private var currentFactoryID = 0
    }

    init{
        val thisMeta = this.itemMeta
        thisMeta?.setDisplayName(title)
        thisMeta?.persistentDataContainer?.set(
            ItemUtil.itemNamespaceKey,
            PersistentDataType.INTEGER,
            currentFactoryID++
        )
        thisMeta?.lore = lore
        if (enchanted)
            thisMeta?.addItemFlags(ItemFlag.HIDE_ENCHANTS)

        this.itemMeta = thisMeta

        // we need to do this after setting the meta, cus this will also change the meta
        if(enchanted)
            addUnsafeEnchantment(Enchantment.DURABILITY, 1)
    }

    // This ID can be used to compare items, for example 2 stone itemsStacks may seem indistinguishable,
    // but with this method you can compare the 2 IDs and see that they are actually not the same stack
    fun getFactoryID() : Int{
          return this.itemMeta!!.persistentDataContainer
             .get(ItemUtil.itemNamespaceKey, PersistentDataType.INTEGER)!!
    }
}