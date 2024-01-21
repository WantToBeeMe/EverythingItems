package me.wanttobee.everythingitems.interactiveinventory

import me.wanttobee.everythingitems.ItemUtil.getFactoryID
import me.wanttobee.everythingitems.UniqueItemStack
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
        val separator = UniqueItemStack(Material.BLACK_STAINED_GLASS_PANE, " ", null)
    }

    init{
        InteractiveInventorySystem.addInventory(this)
    }

    open fun clear(){
        closeViewers()
        InteractiveInventorySystem.removeInventory(this)
    }

    protected abstract var inventory: Inventory
    // we made sure you can only set locks and give click event to Unique items, we don't have to save the whole item anymore
    // instead we can just save their ID's.
    protected val lockedItems : MutableSet<Int> = mutableSetOf(separator.getFactoryID())
    protected val clickEvents : MutableMap<Int, (Player) -> Unit> = mutableMapOf()

    fun itemIsLocked(item : ItemStack) : Boolean{
        return lockedItems.contains(
            item.getFactoryID() ?: return false
        )
    }
    fun itemHasClickEvent(item : ItemStack) : Boolean{
        return clickEvents.containsKey(
            item.getFactoryID() ?: return false
        )
    }

    fun getInternalInventory() : Inventory{
        return inventory
    }
    fun getItem(slot: Int) : ItemStack?{
        return inventory.getItem(slot)
    }
    // this method adds an item to the inventory as if it was a normal inventory,
    // no lock, no interaction, just a normal chest
    fun addSimpleItem(slot: Int, item: ItemStack) {
        return inventory.setItem(slot,item)
    }
    
    // gets the amount of players that are currently looking in this inventory
    fun amountViewers() : Int{
        return inventory.viewers.size
    }

    // checks if the given inventory is the same as this inventory
    fun isThisInventory(check :Inventory?) : Boolean{
        return check == inventory
    }

    // this method gets called whenever a player clicks its own inventory
    // the inventory under the inventory that they opened
    open fun bottomClickEvent(player : Player, event : InventoryClickEvent){
        val item = event.currentItem ?: return

        if(itemIsLocked(item)){
            // we ignore the shiftClick or Left click event because these can ruin the top inventory from below,
            // but you can always override it if you don't agree >:)
            if(event.isShiftClick || event.isLeftClick)
                event.isCancelled = true
        }
    }
    // this method gets called whenever a player clicks on this inventory
    open fun clickEvent(player : Player, event : InventoryClickEvent){
        val item = event.currentItem ?: return

        if(itemIsLocked(item)){
            if(itemHasClickEvent(item)){
                clickEvents[item.getFactoryID()]!!.invoke(player)
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
    fun addLockedItem(slot: Int, item:UniqueItemStack, event:((Player) -> Unit)? = null){
        inventory.setItem(slot, item)
        lockedItems.add(item.getFactoryID())
        if(event != null) clickEvents[item.getFactoryID()] = event
    }
    fun addLockedItem(row : Int, column : Int, item : UniqueItemStack, event :((Player) -> Unit)? = null){
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


    // this method will make sure that all instances from currentItem are swapped with newItem
    // it will also make sure that if it was locked and if it had an event, that these will be passed along
    fun swapItem(currentItemStack : UniqueItemStack, newItemStack : UniqueItemStack){
        val wasLocked = lockedItems.remove(currentItemStack.getFactoryID())
        val event = clickEvents.remove(currentItemStack.getFactoryID())

        for (slot in 0 until inventory.size) {
            if (inventory.getItem(slot) == currentItemStack) {
                inventory.setItem(slot, newItemStack)
            }
        }
        if(wasLocked)
            lockedItems.add(newItemStack.getFactoryID())
        if(event != null)
            clickEvents[newItemStack.getFactoryID()] = event
    }

    fun updateItem(updateItem : UniqueItemStack) : Boolean {
        var anythingDidUpdate = false
        for (slot in 0 until inventory.size) {
            val itemInInv = inventory.getItem(slot) ?: continue
            // we don't have to check for null == null
            // that's because UniqueItemStack always returns a number
            // we should have checked this if it where 2 ItemStacks, but that's not the case
            if(updateItem.getFactoryID() == itemInInv.getFactoryID()){
                anythingDidUpdate = true
                inventory.setItem(slot, updateItem)
            }
        }
        return anythingDidUpdate
    }



    // removes the item from the inventory and removing it from the lockedItems list, and also removes its onClick effect
    fun removeItem(item : UniqueItemStack){
        lockedItems.remove(item.getFactoryID())
        clickEvents.remove(item.getFactoryID())
        for (slot in 0 until inventory.size) {
            val itemInInv = inventory.getItem(slot) ?: continue
            // we don't have to check for null == null
            // that's because UniqueItemStack always returns a number
            // we should have checked this if it where 2 ItemStacks, but that's not the case
            if (itemInInv.getFactoryID() == item.getFactoryID())
                inventory.clear(slot)
        }
    }

    // // don't be confused, this item does not swap the item in that slot only
    // // this method swaps all items that are the same as the one in that slot
    // fun swapItemFromSlot(swapSlot: Int, newItemStack : UniqueItemStack){
    //     val currentItemStack = inventory.getItem(swapSlot) ?: return
    //     currentItemStack.getFactoryID() ?: return
    //     swapItem(currentItemStack as UniqueItemStack, newItemStack)
    // }

    // // don't be confused, this item does not remove the item in that slot only
    // // this method removes all items that are the same as the one in that slot
    // fun removeItemFormSlot(slot: Int){
    //     val currentItemStack = inventory.getItem(slot) ?: return
    //     currentItemStack.getFactoryID() ?: return
    //     removeItem(currentItemStack as UniqueItemStack)
    // }

}
