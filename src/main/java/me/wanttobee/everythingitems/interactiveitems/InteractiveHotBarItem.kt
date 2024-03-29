package me.wanttobee.everythingitems.interactiveitems

import me.wanttobee.everythingitems.IUniqueItemObserver
import me.wanttobee.everythingitems.ItemUtil
import me.wanttobee.everythingitems.UniqueItemStack
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.math.min

// so some background information on how this works
// when you give this item to multiple player it seems like every player contains the exact same instance of the item
// when player A consumes 1 item, it will also be gone for playerB
// However, this is all done by this system, but they are not really the same item.
// When creating an item with the itemFactory, besides the item, they also get a custom id assigned to it
// and because this item contains 1 itemStack that never get switched out, this id always stays the same
// so when playerA makes changes to this item, this change will cause a search through every player that contains an item with the same ID
// and if found, that same change will be applied to them. thus making it look like the same instance

// a lot of methods are open just so you can override them. that does not mean you should though
// I just don't want to ruin your freedom once this is a library, and you can't change the source code
// in almost all cases you want to put the super.myMethod() at the end of the override to make it not break
open class InteractiveHotBarItem(
    // the itemStack is only the visuals, the rest is all functionality
    val itemStack : UniqueItemStack
) : IUniqueItemObserver {

    private lateinit var rightClickEvent : (Player, UniqueItemStack) -> Unit
    private lateinit var leftClickEvent : (Player, UniqueItemStack) -> Unit

    private lateinit var dropEvent : (Player, UniqueItemStack) -> Unit
    private lateinit var swapEvent : (Player, UniqueItemStack) -> Unit

    // when you left or right click, will it take 1 off from the count
    private var consumable = false
    private var countUpCap = 0
    private var countStepSize = 1
    var slot = 0
        private set

    init{
        itemStack.subscribe(this)
        InteractiveHotBarSystem.addItem(this)
    }

    // this method will make sure for everyone that contains this item in there inventory, this item will be taken away.
    // if there is an item in a player that dead, that's not a problem because that is also being handled
    // however, if a player leaves, that is not, so they will join again with a broken item.
    // the item they contain is just a normal item by then and won't work anymore
    open fun clear(){
        itemStack.unsubscribe(this)
        InteractiveHotBarSystem.removeItem(this)
        removeFromEveryone()
    }

    // I don't have tested it, and I am not planning on it because the solution would be cloning which I don't like because it will potentially be a performance hit
    // the thing I am talking about is modifying the ItemStack in all these events, because that is the itemStack we have here
    // however, the only logical modifications someone will make will also afterwords be applied with updateSomething() which solves the issue again
    // so for now I am just saying that you are not allowed to change anything to the ItemStack if you are not planning to update it afterwords

    // note: if you want to change anything about this item, only changing it to that item won't do enough
    // you will have to make sure you use updateCount(), updateMeta() or updateMaterial()
    open fun setRightClickEvent(event : (Player, UniqueItemStack) -> Unit) : InteractiveHotBarItem {
        rightClickEvent = event
        return this
    }
    // note: if you want to change anything about this item, only changing it to that item won't do enough
    // you will have to make sure you use updateCount(), updateMeta() or updateMaterial()
    open fun setLeftClickEvent(event : (Player, UniqueItemStack) -> Unit) : InteractiveHotBarItem {
        leftClickEvent = event
        return this
    }
    // note: a little quirk with dropping items is that they for some reason also will trigger the LeftClick
    // note: if you want to change anything about this item, only changing it to that item won't do enough
    // you will have to make sure you use updateCount(), updateMeta() or updateMaterial()
    open fun setDropEvent(event : (Player, UniqueItemStack) -> Unit): InteractiveHotBarItem {
        dropEvent = event
        return this
    }

    // note: if you want to change anything about this item, only changing it to that item won't do enough
    // you will have to make sure you use updateCount(), updateMeta() or updateMaterial()
    open fun setSwapEvent(event : (Player, UniqueItemStack) -> Unit): InteractiveHotBarItem {
        swapEvent = event
        return this
    }


    //keep in mind, the hotBar is from 0 to 8 (and if you don't set this, it will default to 0)
    fun setSlot(s : Int) : InteractiveHotBarItem {
        slot = s
        return this
    }
    // enabling this will make it so that for everyone who left or right clicks,
    // that item will decrease by one (and if its 1, it will disappear)
    // off by default. use this method to enable it
    // if you also enabled countUpOnUse before, that will be disabled again (only one of the 2 can be active)
    fun setConsumeOnUse() : InteractiveHotBarItem {
        consumable = true
        countUpCap = 0
        return this
    }
    // enabling this will make it so that for everyone who left or right clicks,
    // that item will increase by one (and if it reaches the cap (default 100, max 127), it will go back to 1 to complete the rotation)
    // off by default. use this method to enable it
    // if you also enabled consumeOnUse before, that will be disabled again (only one of the 2 can be active)
    fun setCountUpOnUse(cap: Int = 100) : InteractiveHotBarItem {
        consumable = false
        countUpCap = min(cap, 127)
        return this
    }
    // the default step size is 1
    // the step size is for how fast you increase or decrease (when using countUpOnUse or consumeOnUse)
    fun setCountStepSize(stepSize: Int) : InteractiveHotBarItem {
        countStepSize =stepSize
        return this
    }


    // this will give the item to the player. any modification will still apply to this item.
    // however, keep in mind, that it will enter the that you can provide with setSlot()
    fun giveToPlayer(p : Player){
        p.inventory.setItem(slot, itemStack)
    }

    open fun doRightClickEvent(p : Player){
        if (!::rightClickEvent.isInitialized) return
        rightClickEvent.invoke(p, itemStack)
        if(consumable) decreaseCount(countStepSize)
        if(countUpCap > 0) increaseCount(countStepSize)
    }
    open fun doLeftClickEvent(p : Player){
        if (!::leftClickEvent.isInitialized) return
        leftClickEvent.invoke(p, itemStack)
        if(consumable) decreaseCount(countStepSize)
        if(countUpCap > 0) increaseCount(countStepSize)
    }
    open fun doDropEvent(p : Player){
        if (!::dropEvent.isInitialized) return
        dropEvent.invoke(p, itemStack)
    }
    open fun doSwapEvent(p : Player){
        if (!::swapEvent.isInitialized) return
        swapEvent.invoke(p, itemStack)
    }

    // checking if the itemStack provided is this item
    // we only check if the id is the same, this has 2 benefits.
    // 1. The ID is always unique, so we are 100% sure that it is this item when we do it like this
    // 2. This also allows use to have seamlessly 2 different looks for the same "item" if we manged to create multiple items tacks with the same ID
    fun isThisItem(other : ItemStack?): Boolean{
        return itemStack.equalsID(other)
    }

    // this will remove the item from everyone's inventory
    // but note that it won't remove it from the system, instead call clear() if you want it to also be removed from the system
    fun removeFromEveryone(){
        for(player in ItemUtil.minecraftPlugin.server.onlinePlayers){
            if(isThisItem(player.inventory.getItem(slot))) // we only have to check for the slot we set it to, the item should be in that slot anyway
                player.inventory.setItem(slot, ItemStack(Material.AIR))
        }
    }

    fun getUsageCount() : Int{
        var usageAmount = 0
        for(player in ItemUtil.minecraftPlugin.server.onlinePlayers){
            if(isThisItem(player.inventory.getItem(slot)))
                usageAmount++
        }
        return usageAmount
    }

    fun updateCount(newAmount: Int){
        var newAmount = newAmount
        if(countUpCap > 0 && newAmount > countUpCap){
            newAmount = countStepSize
        }
        itemStack.updateCount(newAmount).pushUpdates()
        // the item will be replaced with the new item in the update callback
    }

    protected fun increaseCount(stepSize : Int = 1){
        updateCount(itemStack.amount + stepSize)
    }
    protected fun decreaseCount(stepSize : Int = 1){
        updateCount(itemStack.amount - stepSize)
    }

    override fun onUniqueItemUpdate(item: UniqueItemStack) {
        for(p in ItemUtil.minecraftPlugin.server.onlinePlayers){
            if( isThisItem( p.inventory.getItem(slot) ) ){
                p.inventory.setItem(slot, item)
            }
        }
    }

    override fun onUniqueItemClear(item: UniqueItemStack) {
        clear()
    }
}
