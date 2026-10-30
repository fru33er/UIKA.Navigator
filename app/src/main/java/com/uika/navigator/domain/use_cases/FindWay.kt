package com.uika.navigator.domain.use_cases

import com.uika.navigator.domain.pathfinding.Path
import com.uika.navigator.domain.pathfinding.Pathfinder
import com.uika.navigator.domain.tree.Tree
import com.uika.navigator.domain.tree.TreeNode

class FindWay(
    private val pathfinder: Pathfinder
) {

    suspend operator fun invoke(
        from: String,
        to: String,
        tree: Tree
    ): Path? {
        return pathfinder.findWay(from, to, tree)
    }
}