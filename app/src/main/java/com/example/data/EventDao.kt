package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startTimestamp ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isImportantDay = 1 ORDER BY startTimestamp ASC")
    fun getImportantDays(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startTimestamp >= :start AND startTimestamp <= :end ORDER BY startTimestamp ASC")
    fun getEventsInRange(start: Long, end: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startTimestamp >= :start AND startTimestamp <= :end ORDER BY startTimestamp ASC")
    fun getEventsBetween(start: Long, end: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE startTimestamp >= :currentTime ORDER BY startTimestamp ASC")
    suspend fun getUpcomingEvents(currentTime: Long): List<EventEntity>

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)
}
