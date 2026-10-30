package com.uika.navigator.data.data_source

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source untuk operasi graf navigasi di Firebase Firestore.
 * Menggantikan GraphDao yang sebelumnya berbasis Room.
 *
 * Struktur Firestore:
 * nodes/ (collection)
 *   {nodeId}/ (document — ID adalah TreeNodeDto.id sebagai String)
 *     id: Int
 *     x: Float
 *     y: Float
 *     z: Float
 *     type: String ("path" | "entry")
 *     number: String? (hanya untuk entry node)
 *     neighbours: List<Int>
 *     forwardVector: Map? {x, y, z, w} (hanya untuk entry node)
 */
@Singleton
class FirestoreGraphDataSource @Inject constructor() {

    val firestore: FirebaseFirestore = Firebase.firestore

    val nodesCollection = firestore.collection("nodes")

}
