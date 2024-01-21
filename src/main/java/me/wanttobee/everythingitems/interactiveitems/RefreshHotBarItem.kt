package me.wanttobee.everythingitems.interactiveitems

import me.wanttobee.everythingitems.ItemUtil

// this is yet another form of an interactive item
// this refresh item allows for easy refreshing of the items look
class RefreshHotBarItem : InteractiveHotBarItem() {
    // the effect on the refresh (it doesn't contain any player because fromm the items perspective it's not know who is holding this)
    private lateinit var refreshEffect : (RefreshHotBarItem) -> Unit
    // the interval set in ticks (time between refreshes)
    private var refreshInterval = -1
    // the id that corresponds to the scheduler.
    // not important for you, but if you must know, it has to be saved to make sure we can cancel the scheduler with this ID once this item is cleared
    private var refreshID = -1

    // note: if you want to change anything about this item, only changing it to that item won't do enough
    // you will have to make sure you use updateCount(), updateMeta() or updateMaterial()
    fun setRefreshEffect(effect : (RefreshHotBarItem) -> Unit) : RefreshHotBarItem {
        refreshEffect = effect
        return this
    }
    // this setting sets how long (in ticks) it takes before this item gets refreshed again
    fun setRefreshInterval(intervalTicks : Int) : RefreshHotBarItem {
        refreshInterval = intervalTicks
        return this
    }

    // you have to call this in order to make the item start refreshing like you specified with the effect and interval
    // returns true if it starts, returns false if it cant start (due to it already running or due to the refreshment not being initialized yet)
    fun startRefreshing() : Boolean{
        if(refreshID != -1 || refreshInterval == -1) return false
        refreshID = ItemUtil.minecraftPlugin.server.scheduler.scheduleSyncRepeatingTask(
            ItemUtil.minecraftPlugin, { doRefresh() },
                0L,
                refreshInterval.toLong()
            )
        return true
    }
    // stops the currently running refreshing loop
    // returns true if it stopped it, returns false if the refresh loop is not running (in that case you wouldn't have to run this method lol)
    fun stopRefreshing() : Boolean{
        if(refreshID != -1) {
            ItemUtil.minecraftPlugin.server.scheduler.cancelTask(refreshID)
            return true
        }
        return false
    }

    // this will invoke the refresh effect you provided
    // this method will be used internally every iteration, however its public if you ever want to trigger it yourself
    fun doRefresh(){
        if(!::refreshEffect.isInitialized) return
        refreshEffect.invoke(this)
    }

    override fun clear() {
        // we add one extra part to the clear method,
        // that is that we want to make sure that all sceduler tasks are canceled
        if(refreshID != -1) ItemUtil.minecraftPlugin.server.scheduler.cancelTask(refreshID)
        super.clear()
    }

}