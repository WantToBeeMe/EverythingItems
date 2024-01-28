package me.wanttobee.everythingitems

interface IUniqueItemObserver {
    fun onUniqueItemUpdate(item: UniqueItemStack)
    fun onUniqueItemClear(item: UniqueItemStack)
}
