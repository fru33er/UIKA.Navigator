package com.uika.navigator.domain.pathfinding

import com.uika.navigator.domain.tree.Tree
import com.uika.navigator.domain.tree.TreeNode

interface Pathfinder {

    suspend fun findWay(
        from: String,
        to: String,
        tree: Tree
    ): Path?

}