package me.wanttobee.everythingitems.interactiveitems

import me.wanttobee.everythingitems.ItemUtil
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

object InteractiveHotBarSystem : Listener {
    private val hotBarItems : MutableList<InteractiveHotBarItem> = mutableListOf()

    // if an item has been added, that means that this item will be taken in account whenever an event happens,
    // this means that whenever someone drops an item, every interactiveItem that has been added will be checked if it was that one
    // that's why it is important to remove the item from the list when you are done with it
    // (but don't worry, InteractiveItem.clear() does that all on its own)
    fun addItem(inv : InteractiveHotBarItem){
        if(!hotBarItems.contains(inv)){
            hotBarItems.add(inv)
        }
    }
    fun removeItem(inv : InteractiveHotBarItem) :Boolean{
        return hotBarItems.remove(inv)
    }
    fun disablePlugin(){
        val hotBarArray = hotBarItems.toTypedArray()
        for(item in hotBarArray){
            item.clear()
        }
    }

    fun debugStatus(commander : Player) {
        commander.sendMessage("${ItemUtil.title} ${ChatColor.YELLOW}active HotBar items:")
        if(hotBarItems.isEmpty())
            commander.sendMessage("${ChatColor.GREEN}there are no HotBar items active")
        for(item in hotBarItems)
            commander.sendMessage("${ChatColor.YELLOW}- ${ChatColor.WHITE}${item::class.simpleName} ${ChatColor.GRAY}(slot: ${item.slot}, usage:${item.getUsageCount()})")
    }

    @EventHandler
    fun onHotBarClick(event: InventoryClickEvent) {
        // we cancel this event if it is done by one of the Interactive items
        // we don't allow for these hot bar items to be changed in your inventory menu
        val item = event.currentItem ?: return
        for(hotBarItem in hotBarItems) {
            if (hotBarItem.isThisItem(item)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler
    fun onHotBarDrag(event: InventoryDragEvent) {
        // we cancel this event if it is done by one of the Interactive items
        // we don't allow for these items to be dragged
        val item = event.cursor ?: return
        for(hotBarItem in hotBarItems) {
            if (hotBarItem.isThisItem(item)) {
                event.isCancelled = true
                return
            }
        }
    }


    @EventHandler
    fun onHotBarDropItem(event: PlayerDropItemEvent) {
        // even though we want players to drop it, we don't really want to actually drop the item,
        // so we cancel the event, but we can still react on it by also calling the DropEvent
        val player = event.player

        val item = event.itemDrop.itemStack
        for(hotBarItem in hotBarItems) {
            if (hotBarItem.isThisItem(item)) {
                hotBarItem.doDropEvent(player)
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler
    fun onSwapHandItems(event: PlayerSwapHandItemsEvent) {
        // even though we want players to swap it, we don't really want to actually swap the item,
        // so we cancel the event, but we can still react on it by also calling the SwapEvent
        val player = event.player
        for(hotBarItem in hotBarItems){
            if(hotBarItem.isThisItem(event.mainHandItem) || hotBarItem.isThisItem((event.offHandItem))){
                hotBarItem.doSwapEvent(player)
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler
    fun onHotBarInteract(event: PlayerInteractEvent) {
        // both Left and Right click are contained in this even because its both interacting
        // we still have to cancel the event because if It's for example a food item, we don't want them to actually eat it
        // but knowing they tried is enough for us to send the event to the item
        val player = event.player
        val action = event.action

        if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            val item = event.item ?: return
            for(hotBarItem in hotBarItems) {
                if (hotBarItem.isThisItem(item)) {
                    hotBarItem.doRightClickEvent(player)
                    event.isCancelled = true
                    return
                }
            }
        }
        if(action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK){
            val item = event.item ?: return
            for(hotBarItem in hotBarItems) {
                if (hotBarItem.isThisItem(item)) {
                    hotBarItem.doLeftClickEvent(player)
                    event.isCancelled = true
                    return
                }
            }
        }
    }


    // these 2 events (death and respawn) are to make sure that players who died will also be cleared this item
    // for some reason when players are dead, there inventory cant be altered
    // if a player died we add that player and the interactiveItems that that person had in to this list
    // then whenever the player respawns, we check all the items in that list and if they are still relevant
    // and if they are not, we remove that item
    private val playerDeathItems: MutableList<Pair<Player, InteractiveHotBarItem>> = mutableListOf()
    @EventHandler
    fun onPlayerDeathHotBarCheck(event: PlayerDeathEvent) {
        val drops = event.drops
        for(hotBarItem in hotBarItems){
            val itemStack = drops.find {item -> hotBarItem.isThisItem(item)} ?: continue
            playerDeathItems.add(Pair(event.entity,hotBarItem))
            drops.remove(itemStack)
        }
    }

    @EventHandler
    fun onPlayerRespawnHotBarCheck(event: PlayerRespawnEvent) {
        val player = event.player
        for(pair in playerDeathItems){
            if(pair.first == player){
                pair.second.giveToPlayer(pair.first)
                playerDeathItems.remove(pair)
                return
            }
        }
    }
}
