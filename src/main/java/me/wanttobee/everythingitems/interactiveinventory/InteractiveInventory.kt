package me.wanttobee.everythingitems.interactiveinventory

import me.wanttobee.everythingitems.IUniqueItemObserver
import me.wanttobee.everythingitems.ItemUtil.getUniqueID
import me.wanttobee.everythingitems.UniqueItemStack
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack


// to create an interactive inventory, you will have to inherit from this abstract class
// everything default setting is set so most of the configurations are already good, however,
// if you want to make some more complex inventories, you might want to change things up a bit, that's why a lot of the methods are overrideable
// for example, you might want to create a build pallet menu, that would require you to make it, so you can put items in and out
// so you might want to override the bottomClickEvent to clone the item you clicked and put a stack of size 1 in the first slot of the pallet. go wild with it
abstract class InteractiveInventory : IUniqueItemObserver{
    companion object{
        // the separator is a black pane glass which is just the default separator. nothing special
        // in theory you could use any itemStack here
        val separator = UniqueItemStack(Material.BLACK_STAINED_GLASS_PANE, " ", null)
    }

    init{
        separator.subscribe(this)
        InteractiveInventorySystem.addInventory(this)
    }

    open fun clear(){
        separator.unsubscribe(this)
        closeViewers()
        InteractiveInventorySystem.removeInventory(this)
    }

    protected abstract var inventory: Inventory
    // we made sure you can only set locks and give click event to Unique items, we don't have to save the whole item anymore
    // instead we can just save their ID's.
    protected val lockedItems : MutableSet<Int> = mutableSetOf(separator.getUniqueID())
    protected val leftClickEvents : MutableMap<Int, (Player) -> Unit> = mutableMapOf()
    protected val rightClickEvents : MutableMap<Int, (Player) -> Unit> = mutableMapOf()

    fun itemIsLocked(item : ItemStack) : Boolean{
        return lockedItems.contains(
            item.getUniqueID() ?: return false
        )
    }
    fun itemHasLeftClickEvent(item : ItemStack) : Boolean{
        return leftClickEvents.containsKey(
            item.getUniqueID() ?: return false
        )
    }
    fun itemHasRightClickEvent(item : ItemStack) : Boolean{
        return rightClickEvents.containsKey(
            item.getUniqueID() ?: return false
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
            val click = event.click
            if(click.isLeftClick && itemHasLeftClickEvent(item) ){
                leftClickEvents[item.getUniqueID()]!!.invoke(player)
            }
            else if(click.isRightClick && itemHasRightClickEvent(item)){
                rightClickEvents[item.getUniqueID()]!!.invoke(player)
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
    fun addLockedItem(slot: Int, item : UniqueItemStack, event:((Player) -> Unit)? = null){
        inventory.setItem(slot, item)
        lockedItems.add(item.getUniqueID())
        item.subscribe(this) // we want to know whenever the item updates, so we can act on that
        if(event != null) {
            // if we specify only 1 event, that means it will happen at both occasions
            leftClickEvents[item.getUniqueID()] = event
            rightClickEvents[item.getUniqueID()] = event
        }
    }
    fun addLockedItem(slot: Int, item : UniqueItemStack, leftClickEvent:((Player) -> Unit)?, rightClickEvent:((Player) -> Unit)?){
        inventory.setItem(slot, item)
        lockedItems.add(item.getUniqueID())
        item.subscribe(this) // we want to know whenever the item updates, so we can act on that
        if(leftClickEvent != null) leftClickEvents[item.getUniqueID()] = leftClickEvent
        if(rightClickEvent != null) rightClickEvents[item.getUniqueID()] = rightClickEvent
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
        val isSeparator = separator.equalsID(currentItemStack)
        val wasLocked = if(isSeparator) true else lockedItems.remove(currentItemStack.getUniqueID())
        val leftEvent = leftClickEvents.remove(currentItemStack.getUniqueID())
        val rightEvent = rightClickEvents.remove(currentItemStack.getUniqueID())

        for (slot in 0 until inventory.size) {
            if (inventory.getItem(slot) == currentItemStack) {
                inventory.setItem(slot, newItemStack)
            }
        }
        if(wasLocked)
            lockedItems.add(newItemStack.getUniqueID())
        if(leftEvent != null) leftClickEvents[newItemStack.getUniqueID()] = leftEvent
        if(rightEvent != null) rightClickEvents[newItemStack.getUniqueID()] = rightEvent
        currentItemStack.unsubscribe(this)
        newItemStack.subscribe(this)
    }

    // removes the item from the inventory
    // and removing it from the lockedItems list
    // and also removes its onClick effect
    fun removeItem(item : UniqueItemStack){
        val isSeparator = separator.equalsID(item)
        if(!isSeparator) lockedItems.remove(item.getUniqueID())
        leftClickEvents.remove(item.getUniqueID())
        rightClickEvents.remove(item.getUniqueID())

        for (slot in 0 until inventory.size) {
            val itemInInv = inventory.getItem(slot) ?: continue
            if (item.equalsID(itemInInv))
                inventory.clear(slot)
        }
        if(!isSeparator) item.unsubscribe(this)
    }

    override fun onUniqueItemUpdate(item: UniqueItemStack) {
        for (slot in 0 until inventory.size) {
            val itemInInv = inventory.getItem(slot) ?: continue
            if(item.equalsID(itemInInv))
                inventory.setItem(slot, item)
        }
    }
    override fun onUniqueItemClear(item: UniqueItemStack) {
        // if the item gets cleared, we don't want to do anything else other than just removing it from the inventory
        removeItem(item)
    }
}
