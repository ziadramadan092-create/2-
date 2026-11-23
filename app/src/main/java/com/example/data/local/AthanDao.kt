package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AthanDao {
  @Query("SELECT * FROM app_settings WHERE id = 1")
  fun observeSettings(): Flow<AppSettingsEntity?>

  @Query("SELECT * FROM app_settings WHERE id = 1")
  suspend fun getSettingsOnce(): AppSettingsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSettings(settings: AppSettingsEntity)

  @Query("SELECT * FROM tasbih_state WHERE id = 1")
  fun observeTasbihState(): Flow<TasbihStateEntity?>

  @Query("SELECT * FROM tasbih_state WHERE id = 1")
  suspend fun getTasbihStateOnce(): TasbihStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTasbihState(state: TasbihStateEntity)

  @Query("SELECT * FROM adhkar_progress WHERE dateKey = :dateKey")
  fun observeAdhkarProgress(dateKey: String): Flow<List<AdhkarProgressEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAdhkarProgress(progress: AdhkarProgressEntity)

  @Query("DELETE FROM adhkar_progress WHERE dateKey = :dateKey AND dhikrId IN (:dhikrIds)")
  suspend fun resetAdhkarProgressForIds(dateKey: String, dhikrIds: List<String>)
}
