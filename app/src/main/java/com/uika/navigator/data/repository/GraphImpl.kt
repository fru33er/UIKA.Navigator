package com.uika.navigator.data.repository

import android.util.Log
import com.uika.navigator.data.data_source.FirestoreGraphDataSource
import com.uika.navigator.data.model.TreeNodeDto
import com.uika.navigator.domain.repository.GraphRepository
import com.uika.navigator.domain.tree.TreeNode
import com.uika.navigator.domain.use_cases.convert
import com.uika.navigator.domain.use_cases.opposite
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.math.toFloat3
import io.github.sceneview.math.toOldQuaternion
import io.github.sceneview.math.toVector3
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GraphImpl @Inject constructor(
    private val dataSource: FirestoreGraphDataSource
) : GraphRepository {

    override suspend fun getNodes(): List<TreeNodeDto> {
        Log.d("GraphImpl", "getNodes() called — fetching from Firestore")
        return try {
            val snapshot = dataSource.nodesCollection.get().await()
            val nodes = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                TreeNodeDto.fromDocument(doc.id, data)
            }
            Log.d("GraphImpl", "getNodes() returned ${nodes.size} nodes from Firestore")
            nodes
        } catch (e: Exception) {
            Log.e("GraphImpl", "getNodes() failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun insertNodes(
        nodes: List<TreeNode>,
        translocation: Float3,
        rotation: Quaternion,
        pivotPosition: Float3
    ) {
        Log.d("GraphImpl", "insertNodes() called with ${nodes.size} nodes")

        val undoTranslocation = translocation * -1f
        val undoQuaternion = rotation.opposite()

        val dtoNodes = nodes.map { node ->
            when (node) {
                is TreeNode.Entry -> TreeNodeDto.fromTreeNode(
                    node = node,
                    position = undoPositionConvert(node.position, undoTranslocation, undoQuaternion, pivotPosition),
                    forwardVector = node.forwardVector.convert(undoQuaternion)
                )
                is TreeNode.Path -> TreeNodeDto.fromTreeNode(
                    node = node,
                    position = undoPositionConvert(node.position, undoTranslocation, undoQuaternion, pivotPosition)
                )
            }
        }

        writeBatch(dtoNodes)
        Log.d("GraphImpl", "insertNodes() completed — ${dtoNodes.size} nodes uploaded to Firestore")
    }

    override suspend fun deleteNodes(nodes: List<TreeNode>) {
        Log.d("GraphImpl", "deleteNodes() called with ${nodes.size} nodes")
        try {
            // Chunk 500 karena Firestore batch limit adalah 500 operasi
            nodes.chunked(500).forEach { chunk ->
                val batch = dataSource.firestore.batch()
                chunk.forEach { node ->
                    batch.delete(dataSource.nodesCollection.document(node.id.toString()))
                }
                batch.commit().await()
            }
            Log.d("GraphImpl", "deleteNodes() completed")
        } catch (e: Exception) {
            Log.e("GraphImpl", "deleteNodes() failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun updateNodes(
        nodes: List<TreeNode>,
        translocation: Float3,
        rotation: Quaternion,
        pivotPosition: Float3
    ) {
        Log.d("GraphImpl", "updateNodes() called with ${nodes.size} nodes")

        val undoTranslocation = translocation * -1f
        val undoQuaternion = rotation.opposite()

        val dtoNodes = nodes.map { node ->
            when (node) {
                is TreeNode.Entry -> TreeNodeDto.fromTreeNode(
                    node = node,
                    position = undoPositionConvert(node.position, undoTranslocation, undoQuaternion, pivotPosition),
                    forwardVector = node.forwardVector.convert(undoQuaternion)
                )
                is TreeNode.Path -> TreeNodeDto.fromTreeNode(
                    node = node,
                    position = undoPositionConvert(node.position, undoTranslocation, undoQuaternion, pivotPosition)
                )
            }
        }

        writeBatch(dtoNodes)
        Log.d("GraphImpl", "updateNodes() completed")
    }

    override suspend fun clearNodes() {
        Log.d("GraphImpl", "clearNodes() called — deleting all nodes from Firestore")
        try {
            val snapshot = dataSource.nodesCollection.get().await()
            snapshot.documents.chunked(500).forEach { chunk ->
                val batch = dataSource.firestore.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
            Log.d("GraphImpl", "clearNodes() completed — ${snapshot.size()} nodes deleted")
        } catch (e: Exception) {
            Log.e("GraphImpl", "clearNodes() failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Tulis daftar TreeNodeDto ke Firestore dalam batch (maks 500 per batch).
     */
    private suspend fun writeBatch(dtoNodes: List<TreeNodeDto>) {
        dtoNodes.chunked(500).forEach { chunk ->
            val batch = dataSource.firestore.batch()
            chunk.forEach { dto ->
                val docRef = dataSource.nodesCollection.document(dto.id.toString())
                batch.set(docRef, dto.toMap())
            }
            batch.commit().await()
        }
    }

    private fun undoPositionConvert(
        position: Float3,
        translocation: Float3,
        quaternion: Quaternion,
        pivotPosition: Float3
    ): Float3 {
        return (com.google.ar.sceneform.math.Quaternion.rotateVector(
            quaternion.toOldQuaternion(),
            (position - pivotPosition - translocation).toVector3()
        ).toFloat3() + pivotPosition)
    }
}
