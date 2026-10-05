package com.example.spaceplan.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val dataStore: DataStore<Preferences>) {

    private val key = stringPreferencesKey("history_records")

    val history: Flow<List<HistoryRecord>> = dataStore.data.map { HistoryCodec.decode(it[key].orEmpty()) }

    suspend fun add(record: HistoryRecord) {
        dataStore.edit { prefs ->
            prefs[key] = HistoryCodec.encode(HistoryCodec.addRecord(HistoryCodec.decode(prefs[key].orEmpty()), record))
        }
    }

    suspend fun delete(id: Long) {
        dataStore.edit { prefs ->
            prefs[key] = HistoryCodec.encode(HistoryCodec.decode(prefs[key].orEmpty()).filter { it.id != id })
        }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
