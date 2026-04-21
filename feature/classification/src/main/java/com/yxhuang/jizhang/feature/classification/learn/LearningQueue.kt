package com.yxhuang.jizhang.feature.classification.learn

object LearningQueue {
    private val queue = mutableSetOf<String>()

    @Synchronized
    fun enqueue(merchant: String) {
        queue.add(merchant)
    }

    @Synchronized
    fun dequeueAll(): List<String> {
        val copy = queue.toList()
        queue.clear()
        return copy
    }
}
