package me.wanttobee.everythingitems.interactiveinventory

import me.wanttobee.everythingitems.ItemUtil
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

abstract class IInventoryMenu {
    companion object{
        // the separator is a black pane glass which is just the default separator. nothing special
        // in theory you could use any itemStack here
        val separator = ItemUtil.itemFactory(Material.BLACK_STAINED_GLASS_PANE, " ", null)
    }

    protected abstract var inventory: Inventory
    protected val lockedItems : MutableList<ItemStack> = mutableListOf()
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
                inventory.setItem(slot, separator)
            }
        }
    }

    fun removeItem(item : ItemStack){
        lockedItems.remove(item)
        clickEvents.remove(item)
    }


}