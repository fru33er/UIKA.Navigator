package com.uika.navigator.domain.di

import android.app.Application
import androidx.room.Room
import com.uika.navigator.data.data_source.Database
import com.uika.navigator.data.data_source.FirestoreGraphDataSource
import com.uika.navigator.data.ml.classification.TextAnalyzer
import com.uika.navigator.data.pathfinding.AStarImpl
import com.uika.navigator.data.repository.GraphImpl
import com.uika.navigator.data.repository.RecordsImpl
import com.uika.navigator.domain.ml.ObjectDetector
import com.uika.navigator.domain.pathfinding.Pathfinder
import com.uika.navigator.domain.repository.GraphRepository
import com.uika.navigator.domain.repository.RecordsRepository
import com.uika.navigator.domain.tree.Tree
import com.uika.navigator.domain.use_cases.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val DATABASE_NAME = "UIKA_DB.db"

    /**
     * Room Database hanya untuk menyimpan Record (riwayat pencarian).
     * Data graf navigasi sekarang disimpan di Firebase Firestore.
     * fallbackToDestructiveMigration diperlukan karena schema berubah dari v1 ke v2
     * (TreeNodeDto dihapus dari Room).
     */
    @Provides
    @Singleton
    fun provideDatabase(app: Application): Database {
        return Room.databaseBuilder(app, Database::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Firestore data source untuk operasi graf navigasi (node).
     */
    @Provides
    @Singleton
    fun provideFirestoreGraphDataSource(): FirestoreGraphDataSource {
        return FirestoreGraphDataSource()
    }

    @Provides
    @Singleton
    fun provideGraphRepository(dataSource: FirestoreGraphDataSource): GraphRepository {
        return GraphImpl(dataSource)
    }

    @Provides
    @Singleton
    fun provideRecordsRepository(database: Database): RecordsRepository {
        return RecordsImpl(database)
    }

    @Provides
    @Singleton
    fun provideTree(repository: GraphRepository): Tree {
        return Tree(repository)
    }

    @Provides
    @Singleton
    fun provideSmoothPath(): SmoothPath {
        return SmoothPath()
    }

    @Provides
    @Singleton
    fun providePathfinder(smoothPath: SmoothPath): Pathfinder {
        return AStarImpl(smoothPath)
    }

    @Provides
    @Singleton
    fun provideFindWay(pathfinder: Pathfinder): FindWay {
        return FindWay(pathfinder)
    }

    @Provides
    @Singleton
    fun provideObjectDetector(): ObjectDetector {
        return TextAnalyzer()
    }

    @Provides
    @Singleton
    fun provideImageAnalyzer(objectDetector: ObjectDetector): AnalyzeImage {
        return AnalyzeImage(objectDetector)
    }

    @Provides
    @Singleton
    fun provideHitTest(): HitTest {
        return HitTest()
    }

    @Provides
    @Singleton
    fun provideDestinationDest(): GetDestinationDesc {
        return GetDestinationDesc()
    }

}
