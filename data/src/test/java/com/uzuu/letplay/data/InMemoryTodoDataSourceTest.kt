package com.uzuu.letplay.data

import com.uzuu.letplay.data.datasource.InMemoryTodoDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryTodoDataSourceTest {
    @Test
    fun concurrentWritesPreserveEveryTodoAndToggle() = runTest {
        val source = InMemoryTodoDataSource()
        (1..100).map { index -> async(Dispatchers.Default) { source.addTodo("Todo $index") } }.awaitAll()
        assertEquals(102, source.getTodos().size)
        val initial = source.getTodos().first { it.id == "1" }.isCompleted
        (1..100).map { async(Dispatchers.Default) { source.toggleTodo("1") } }.awaitAll()
        assertEquals(initial, source.getTodos().first { it.id == "1" }.isCompleted)
    }
}
