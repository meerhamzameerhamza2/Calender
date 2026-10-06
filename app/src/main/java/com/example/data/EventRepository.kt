package com.example.data

import com.example.model.EventEntity
import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {

    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()
    val importantDays: Flow<List<EventEntity>> = eventDao.getImportantDays()

    fun getEventsBetween(start: Long, end: Long): Flow<List<EventEntity>> {
        return eventDao.getEventsBetween(start, end)
    }

    suspend fun getEventById(id: Long): EventEntity? {
        return eventDao.getEventById(id)
    }

    suspend fun getUpcomingEvents(currentTime: Long): List<EventEntity> {
        return eventDao.getUpcomingEvents(currentTime)
    }

    suspend fun insertEvent(event: EventEntity): Long {
        return eventDao.insertEvent(event)
    }

    suspend fun updateEvent(event: EventEntity) {
        eventDao.updateEvent(event)
    }

    suspend fun deleteEvent(event: EventEntity) {
        eventDao.deleteEvent(event)
    }

    suspend fun deleteEventById(id: Long) {
        eventDao.deleteEventById(id)
    }
}
