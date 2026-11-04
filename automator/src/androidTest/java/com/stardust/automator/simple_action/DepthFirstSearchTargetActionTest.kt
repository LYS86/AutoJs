package com.stardust.automator.simple_action

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.stardust.automator.UiObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthFirstSearchTargetActionTest {

    private class Node(
        private val children: List<Node> = emptyList(),
        private val clickable: Boolean = false
    ) : UiObject(null) {

        val actions = mutableListOf<Int>()

        override fun child(i: Int): UiObject? = children.getOrNull(i)

        override fun getChildCount(): Int = children.size

        override fun isClickable(): Boolean = clickable

        override fun performAction(action: Int): Boolean {
            actions.add(action)
            return true
        }

        override fun performAction(action: Int, bundle: Bundle): Boolean = performAction(action)

        override fun recycle() = Unit

        override fun equals(other: Any?): Boolean = this === other

        override fun hashCode(): Int = System.identityHashCode(this)

        override fun toString(): String = "Node(clickable=$clickable)"
    }

    private fun childrenFilter() = object : FilterAction.Filter {
        override fun filter(root: UiObject): List<UiObject> {
            val list = ArrayList<UiObject>()
            for (i in 0 until root.childCount()) {
                root.child(i)?.let { list.add(it) }
            }
            return list
        }
    }

    @Test
    fun performsActionOnEveryClickableNodeFoundDepthFirst() {
        val deepTarget = Node(clickable = true)
        val middle = Node(listOf(deepTarget))
        val first = Node(listOf(middle))
        val second = Node(clickable = true)
        val root = Node(listOf(first, second))

        val action = DepthFirstSearchTargetAction(AccessibilityNodeInfo.ACTION_CLICK, childrenFilter())

        assertTrue(action.perform(root))
        assertEquals(listOf(AccessibilityNodeInfo.ACTION_CLICK), deepTarget.actions)
        assertEquals(listOf(AccessibilityNodeInfo.ACTION_CLICK), second.actions)
    }

    @Test
    fun returnsFalseWhenNoClickableNodeExists() {
        val root = Node(listOf(Node(listOf(Node()))))

        val action = DepthFirstSearchTargetAction(AccessibilityNodeInfo.ACTION_CLICK, childrenFilter())

        assertFalse(action.perform(root))
    }
}
