package me.wanttobee.everythingitems

import me.wanttobee.everythingitems.ItemUtil.getUniqueID
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
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

    // TODO:
    //  if it turns out that cloning this list on each update call will result in lag, a solution could be
    //  to instead not clone observers list, and instead have a boolean indicating if we can (un)subscribe
    //  and if this is true we (un)subscribe like normal, and if its false, we instead add the (un)subscribe request to a buffer
    //  then whenever the boolean is set from false to true again, we then go through the buffer and apply the changes
    //  This would be a lot more effort to code, but would be more efficient
    private val itemObservers : MutableSet<IUniqueItemObserver> = mutableSetOf()
    fun subscribe(observer: IUniqueItemObserver) : Boolean{
        return itemObservers.add(observer)
    }
    fun unsubscribe(observer: IUniqueItemObserver) : Boolean{
        return itemObservers.remove(observer)
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
    fun getUniqueID() : Int{
          return this.itemMeta!!.persistentDataContainer
             .get(ItemUtil.itemNamespaceKey, PersistentDataType.INTEGER)!!
    }
    fun equalsID(other : ItemStack?) : Boolean{
        return getUniqueID() == other?.getUniqueID()
    }

    fun pushUpdates(){
        for(itemObserver in itemObservers){
            itemObserver.onUniqueItemUpdate(this)
        }
    }

    fun clearItem(){
        for(itemObserver in itemObservers){
            itemObserver.onUniqueItemClear(this)
        }
    }

    fun updateMeta(newMeta : ItemMeta) : UniqueItemStack{
        val newUniqueID = newMeta.persistentDataContainer.get(ItemUtil.itemNamespaceKey, PersistentDataType.INTEGER)
        // we are not going to update the meta if the new ID is not the same
        if(newUniqueID != this.getUniqueID())
            return this

        this.itemMeta = newMeta
        return this
    }
    fun updateTitle(newTitle: String) : UniqueItemStack{
        val thisMeta = this.itemMeta!!
        thisMeta.setDisplayName(newTitle)
        this.itemMeta = thisMeta
        return this
    }
    fun updateLore(newLore: List<String>?) : UniqueItemStack{
        val thisMeta = this.itemMeta!!
        thisMeta.lore = newLore
        this.itemMeta = thisMeta
        return this
    }
    fun updateCustomModelData(newModelData: Int) : UniqueItemStack {
        val thisMeta = this.itemMeta!!
        thisMeta.setCustomModelData(newModelData)
        this.itemMeta = thisMeta
        return this
    }
    // this will give a glint to the item, without showing the real enchantment that has been applied
    // entering false in this method will remove the enchantment, but also the hide flag so real enchantments can be seen again
    fun updateEnchanted(newEnchanted: Boolean) : UniqueItemStack{
        val thisMeta = this.itemMeta!!
        if(newEnchanted) thisMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        else thisMeta.removeItemFlags(ItemFlag.HIDE_ENCHANTS)
        this.itemMeta = thisMeta

        if(newEnchanted) addUnsafeEnchantment(Enchantment.DURABILITY, 1)
        else removeEnchantment(Enchantment.DURABILITY)

        return this
    }
    fun updateMaterial(newMaterial: Material) : UniqueItemStack{
        this.type = newMaterial
        return this
    }
    fun updateCount(newAmount: Int) : UniqueItemStack{
        this.amount = newAmount
        return this
    }
    fun increaseCount(steps : Int = 1) : UniqueItemStack{
        return updateCount(this.amount+ steps)
    }
    fun decreaseCount(steps : Int = 1) : UniqueItemStack{
        return updateCount(this.amount - steps)
    }

    // these are the exact same as the methods, however they are easier to write :P
    operator fun dec() : UniqueItemStack {
        return decreaseCount()
    }
    operator fun inc() : UniqueItemStack {
        return increaseCount()
    }
}
