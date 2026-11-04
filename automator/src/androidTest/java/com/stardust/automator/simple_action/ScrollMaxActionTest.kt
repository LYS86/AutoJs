package com.stardust.automator.simple_action

import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.stardust.automator.UiObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollMaxActionTest {

    private class Node(
        private val children: List<Node> = emptyList(),
        private val scrollable: Boolean = false,
        private val width: Int = 0,
        private val height: Int = 0
    ) : UiObject(null) {

        val actions = mutableListOf<Int>()
        var recycleCount = 0

        override fun child(i: Int): UiObject? = children.getOrNull(i)

        override fun getChildCount(): Int = children.size

        override fun isScrollable(): Boolean = scrollable

        override fun getBoundsInScreen(outBounds: Rect) {
            outBounds.set(0, 0, width, height)
        }

        override fun performAction(action: Int): Boolean {
            actions.add(action)
            return true
        }

        override fun performAction(action: Int, bundle: Bundle): Boolean = performAction(action)

        override fun recycle() {
            recycleCount++
        }

        override fun equals(other: Any?): Boolean = this === other

        override fun hashCode(): Int = System.identityHashCode(this)

        override fun toString(): String = "Node(${width}x$height, scrollable=$scrollable)"
    }

    @Test
    fun picksLargestScrollableNodeAndRecyclesTheRest() {
        val small = Node(scrollable = true, width = 10, height = 10)
        val big = Node(scrollable = true, width = 30, height = 30)
        val medium = Node(scrollable = true, width = 20, height = 20)
        val root = Node(listOf(small, big, medium))

        val action = ScrollMaxAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)

        assertTrue(action.perform(root))
        assertEquals(listOf(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD), big.actions)
        assertTrue(small.actions.isEmpty())
        assertTrue(medium.actions.isEmpty())
        assertEquals(1, small.recycleCount)
        assertEquals(1, medium.recycleCount)
        assertEquals(1, big.recycleCount)
        assertEquals(0, root.recycleCount)
    }

    @Test
    fun keepsRootWhenRootIsTheLargestScrollableNode() {
        val small = Node(scrollable = true, width = 10, height = 10)
        val root = Node(listOf(small), scrollable = true, width = 100, height = 100)

        val action = ScrollMaxAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)

        assertTrue(action.perform(root))
        assertEquals(listOf(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD), root.actions)
        assertEquals(0, root.recycleCount)
        assertEquals(1, small.recycleCount)
    }

    @Test
    fun returnsFalseWhenNothingIsScrollable() {
        val root = Node(listOf(Node(), Node()))

        val action = ScrollMaxAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)

        assertFalse(action.perform(root))
    }
}
