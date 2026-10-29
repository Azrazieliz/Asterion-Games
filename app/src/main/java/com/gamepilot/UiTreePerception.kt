package com.gamepilot

import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * Direct Android UI-tree perception. Works for accessible native UI without screen capture,
 * but most games use a GPU surface with no accessible internal controls.
 */
object UiTreePerception {
    fun snapshot(root: AccessibilityNodeInfo?, metrics: DisplayMetrics): LiveFrame? {
        if (root == null) return null
        val nodes = ArrayDeque<AccessibilityNodeInfo>()
        nodes.add(root)
        val labels = mutableListOf<SeenText>()
        var seen = 0
        try {
            while (nodes.isNotEmpty() && seen < 350) {
                val node = nodes.removeFirst()
                seen++
                try {
                    val title = node.text?.toString()?.takeIf { it.isNotBlank() }
                        ?: node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
                    if (title != null && title.length <= 150) {
                        val box = Rect()
                        node.getBoundsInScreen(box)
                        if (!box.isEmpty) labels.add(SeenText(title, box))
                    }
                    for (i in 0 until node.childCount) {
                        val child = node.getChild(i)
                        if (child != null) nodes.addLast(child)
                    }
                } finally { node.recycle() }
            }
        } finally {
            while (nodes.isNotEmpty()) nodes.removeFirst().recycle()
        }
        if (labels.isEmpty()) return null
        return LiveFrame(System.currentTimeMillis(),
            metrics.widthPixels, metrics.heightPixels,
            IntArray(96), labels.distinctBy { it.label to it.bounds.toShortString() })
    }
}
