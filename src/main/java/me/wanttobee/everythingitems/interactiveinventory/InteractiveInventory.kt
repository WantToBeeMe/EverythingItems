package me.wanttobee.everythingitems.interactiveinventory

import me.wanttobee.everythingitems.ItemUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack


// to create an interactive inventory, you will have to inherit from this abstract class
// everything default setting is set so most of the configurations are already good, however,
// if you want to make some more complex inventories, you might want to change things up a bit, that's why a lot of the methods are overrideable
// for example, you might want to create a build pallet menu, that would require you to make it, so you can put items in and out
// so you might want to override the bottomClickEvent to clone the item you clicked and put a stack of size 1 in the first slot of the pallet. go wild with it
abstract class InteractiveInventory {
    companion object{
        // the separator is a black pane glass which is just the default separator. nothing special
        // in theory you could use any itemStack here
        val separator = ItemUtil.itemFactory(Material.BLACK_STAINED_GLASS_PANE, " ", null)

        // both these methods are just to make your life a tad easier when creating an interactive inventory
        // its private, so you don't use it anywhere else, cus that would be really weird if you do it somewhere where this interactiveInventory doesn't belong
        private fun createInventory(slots : Int, title: String) : Inventory{
            return Bukkit.createInventory(null, slots, title)
        }
        private fun createInventory(type : InventoryType, title: String) : Inventory{
            return Bukkit.createInventory(null, InventoryType.DROPPER, title)
        }
    }

    init{
        InteractiveInventorySystem.addInventory(this)
    }

    open fun clear(){
        closeViewers()
        InteractiveInventorySystem.removeInventory(this)
    }

    protected abstract var inventory: Inventory
    protected val lockedItems : MutableSet<ItemStack> = mutableSetOf(separator)
    protected val clickEvents : MutableMap<ItemStack, (Player) -> Unit> = mutableMapOf()

    // gets the amount of players that are currently looking in this inventory
    fun amountViewers() : Int{
        return inventory.viewers.size
    }

    // returns true if the given inventory is the same as this inventory
    fun isThisInventory(check :Inventory?) : Boolean{
        return check == inventory
    }

    // this method gets called whenever a player clicks its own inventory
    // the inventory under the inventory that they opened
    open fun bottomClickEvent(player : Player, event : InventoryClickEvent){
        val item = event.currentItem ?: return
        val itemWithoutStackSize = item.clone()
        itemWithoutStackSize.amount = 1

        if(lockedItems.contains(itemWithoutStackSize)){
            if(event.isShiftClick || event.isLeftClick)
                event.isCancelled = true
        }
    }
    // this method gets called whenever a player clicks on this inventory
    open fun clickEvent(player : Player, event : InventoryClickEvent){
        val item = event.currentItem ?: return

        if(lockedItems.contains(item)){
            if(clickEvents.containsKey(item)){
                clickEvents[item]!!.invoke(player)
            }
            event.isCancelled = true
        }
    }

    // this method gets called whenever a player drags in its own inventory
    // the inventory under the inventory that they opened
    open fun bottomDragEvent(player : Player, event: InventoryDragEvent){}
    // this method gets called whenever a player drags in this inventory
    open fun dragEvent(player : Player, event: InventoryDragEvent){}

    // whenever a player closes this inventory, this will be called
    open fun closeEvent(player : Player, event : InventoryCloseEvent){}

    // opens the inventory to the specified player
    fun open(player : Player){
        player.openInventory(inventory)
    }
    // closes this inventory for everyone currently look in this inventory
    fun closeViewers(){
        for (viewerID in inventory.viewers.indices) {
            if (inventory.viewers.size > viewerID
                && inventory.viewers[viewerID] is Player
                && inventory.viewers[viewerID].openInventory.topInventory == inventory) {
                inventory.viewers[viewerID].closeInventory()
            }
        }
    }

    // the inventory acts like normal, you can put items in it and take items out
    // however, you can add and delete items that act like a menu and thus are locked
    // having this cool feature that both locked and non-locked items can co-exist means you can create really cool stuff
    fun addLockedItem(slot: Int, item:ItemStack, event:((Player) -> Unit)? = null){
        inventory.setItem(slot, item)
        lockedItems.add(item)
        if(event != null) clickEvents[item] = event
    }
    fun addLockedItem(row : Int, column : Int, item : ItemStack, event :((Player) -> Unit)? = null){
        return addLockedItem(row*9 + column, item,event)
    }

    // fills al the empty slots with the separator so that there are no more empty slots left
    fun fillGapsWithSeparator(){
        for (slot in 0 until inventory.size) {
            if (inventory.getItem(slot) == null) {
                addSeparator(slot)
            }
        }
    }
    fun addSeparator(slot: Int){
        inventory.setItem(slot, separator)
        // we don't have to lock it because they are in the locked list by default
    }


    // this method edits a given item in the inventory, going from the currentItemStack to the newItemStack
    fun swapItem(currentItemStack : ItemStack, newItemStack : ItemStack){
        val wasLocked = lockedItems.remove(currentItemStack)
        val event = clickEvents.remove(currentItemStack)

        for (slot in 0 until inventory.size) {
            if (inventory.getItem(slot) == currentItemStack) {
                inventory.setItem(slot, newItemStack)
            }
        }
        if(wasLocked)
            lockedItems.add(newItemStack)
        if(event != null)
            clickEvents[newItemStack] = event
    }
    
    // don't be confused, this item does not swap the item in that slot only
    // this method swaps all items that are the same as the one in that slot
    fun swapItemFromSlot(swapSlot: Int, newItemStack : ItemStack){
        val currentItemStack = inventory.getItem(swapSlot) ?:  return
        swapItem(currentItemStack, newItemStack)
    }

    // removes the item from the inventory and removing it from the lockedItems list, and also removes its onClick effect
    fun removeItem(item : ItemStack){
        lockedItems.remove(item)
        clickEvents.remove(item)
        for (slot in 0 until inventory.size) {
            if (inventory.getItem(slot) == item)
                inventory.clear(slot)
        }
    }

}