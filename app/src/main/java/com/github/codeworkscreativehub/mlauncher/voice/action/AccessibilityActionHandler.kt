package com.github.codeworkscreativehub.mlauncher.voice.action

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.fuzzywuzzy.FuzzyFinder
import java.util.LinkedList

/**
 * Performs accessibility-based UI interactions using BFS traversal of the
 * accessibility node tree.
 *
 * Algorithm: BFS (Breadth-First Search) over the accessibility node tree
 * - Time: O(N) where N = total nodes in the tree
 * - Space: O(W) where W = max width of the tree (queue size)
 *
 * Uses fuzzy matching to find nodes by text/contentDescription,
 * enabling voice commands like "tap Settings" or "click Submit".
 */
object AccessibilityActionHandler {

    private const val TAG = "AccessibilityAction"
    private const val FUZZY_THRESHOLD = 40

    /**
     * BFS traversal to find and click a node matching the description.
     * Fuzzy-matches against both text and contentDescription of each node.
     */
    fun findAndClickNode(service: AccessibilityService, description: String): ActionResult {
        val rootNode = service.rootInActiveWindow
            ?: return ActionResult.Failed("No active window")

        val bestMatch = findBestMatchingNode(rootNode, description)

        return if (bestMatch != null) {
            val clicked = bestMatch.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            bestMatch.recycle()
            rootNode.recycle()
            if (clicked) ActionResult.Success
            else ActionResult.Failed("Node found but click failed")
        } else {
            rootNode.recycle()
            ActionResult.Failed("No matching UI element found for: $description")
        }
    }

    /**
     * Types text into the currently focused input field.
     */
    fun typeIntoFocused(service: AccessibilityService, text: String): ActionResult {
        val rootNode = service.rootInActiveWindow
            ?: return ActionResult.Failed("No active window")

        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode == null) {
            rootNode.recycle()
            return ActionResult.Failed("No focused input field")
        }

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        focusedNode.recycle()
        rootNode.recycle()

        return if (success) ActionResult.Success
        else ActionResult.Failed("Failed to type text")
    }

    /**
     * Reads visible text nodes from the screen for LLM context.
     * Uses BFS to collect all text content.
     */
    fun getScreenContent(service: AccessibilityService): String {
        val rootNode = service.rootInActiveWindow ?: return ""
        val textContent = StringBuilder()
        val queue: LinkedList<AccessibilityNodeInfo> = LinkedList()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.poll() ?: continue

            node.text?.let { text ->
                if (text.isNotBlank()) {
                    textContent.appendLine(text)
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        rootNode.recycle()
        return textContent.toString().take(2000) // Cap at 2000 chars
    }

    /**
     * BFS traversal finding the best fuzzy-matching node.
     */
    private fun findBestMatchingNode(
        root: AccessibilityNodeInfo,
        description: String
    ): AccessibilityNodeInfo? {
        val queue: LinkedList<AccessibilityNodeInfo> = LinkedList()
        queue.add(root)

        var bestNode: AccessibilityNodeInfo? = null
        var bestScore = FUZZY_THRESHOLD

        while (queue.isNotEmpty()) {
            val node = queue.poll() ?: continue

            // Score against text and contentDescription
            val nodeText = node.text?.toString() ?: ""
            val nodeDesc = node.contentDescription?.toString() ?: ""

            val textScore = if (nodeText.isNotBlank()) {
                FuzzyFinder.scoreString(nodeText, description, 100)
            } else 0

            val descScore = if (nodeDesc.isNotBlank()) {
                FuzzyFinder.scoreString(nodeDesc, description, 100)
            } else 0

            val maxScore = maxOf(textScore, descScore)

            if (maxScore > bestScore && node.isClickable) {
                bestNode?.recycle()
                bestNode = AccessibilityNodeInfo.obtain(node)
                bestScore = maxScore
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return bestNode
    }
}
