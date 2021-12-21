package com.jersonchua.secondaryindex

inline fun <reified U : T, reified V : T, reified T> Iterable<T>.partitionByTypes(): Triple<List<U>, List<V>, List<T>> {
    val first = ArrayList<U>()
    val second = ArrayList<V>()
    val third = ArrayList<T>()
    for (element in this) {
        when (element) {
            is U -> first.add(element)
            is V -> second.add(element)
            else -> third.add(element)
        }
    }
    return Triple(first, second, third)
}