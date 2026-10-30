package com.uika.navigator.presentation.preview.state

import com.uika.navigator.domain.pathfinding.Path
import com.uika.navigator.domain.tree.TreeNode
import com.uika.navigator.presentation.LabelObject

data class PathState(
    val startEntry: TreeNode.Entry? = null,
    val endEntry: TreeNode.Entry? = null,
    val path: Path? = null
)
