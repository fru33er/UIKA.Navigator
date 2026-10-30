package com.uika.navigator.data.model

import com.uika.navigator.domain.tree.TreeNode
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

/**
 * Data Transfer Object untuk node navigasi.
 * Tidak lagi menggunakan Room annotations — data disimpan di Firebase Firestore.
 */
data class TreeNodeDto(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val type: String = TYPE_PATH,
    val number: String? = null,
    val neighbours: MutableList<Int> = mutableListOf(),
    val forwardVector: Quaternion? = null
) {
    /**
     * Konversi ke Map untuk upload ke Firestore.
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "x" to x,
        "y" to y,
        "z" to z,
        "type" to type,
        "number" to number,
        "neighbours" to neighbours,
        "forwardVector" to forwardVector?.let {
            mapOf("x" to it.x, "y" to it.y, "z" to it.z, "w" to it.w)
        }
    )

    companion object {
        const val TYPE_PATH = "path"
        const val TYPE_ENTRY = "entry"

        /**
         * Buat TreeNodeDto dari Firestore document data.
         */
        fun fromDocument(docId: String, data: Map<String, Any>): TreeNodeDto? {
            val id = docId.toIntOrNull() ?: return null
            return TreeNodeDto(
                id = id,
                x = (data["x"] as? Double)?.toFloat() ?: 0f,
                y = (data["y"] as? Double)?.toFloat() ?: 0f,
                z = (data["z"] as? Double)?.toFloat() ?: 0f,
                type = data["type"] as? String ?: TYPE_PATH,
                number = data["number"] as? String,
                neighbours = ((data["neighbours"] as? List<*>)
                    ?.mapNotNull { (it as? Long)?.toInt() }
                    ?.toMutableList()) ?: mutableListOf(),
                forwardVector = (data["forwardVector"] as? Map<*, *>)?.let { map ->
                    val fx = (map["x"] as? Double)?.toFloat() ?: return@let null
                    val fy = (map["y"] as? Double)?.toFloat() ?: return@let null
                    val fz = (map["z"] as? Double)?.toFloat() ?: return@let null
                    val fw = (map["w"] as? Double)?.toFloat() ?: return@let null
                    Quaternion(fx, fy, fz, fw)
                }
            )
        }

        fun fromTreeNode(
            node: TreeNode,
            position: Float3? = null,
            forwardVector: Quaternion? = null
        ): TreeNodeDto {
            return TreeNodeDto(
                id = node.id,
                x = position?.x ?: node.position.x,
                y = position?.y ?: node.position.y,
                z = position?.z ?: node.position.z,
                forwardVector = forwardVector ?: if (node is TreeNode.Entry) node.forwardVector else null,
                type = if (node is TreeNode.Entry) TYPE_ENTRY else TYPE_PATH,
                number = if (node is TreeNode.Entry) node.number else null,
                neighbours = node.neighbours
            )
        }
    }
}