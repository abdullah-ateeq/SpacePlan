package com.example.spaceplan.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.spaceplan.domain.EdgeGapMode
import com.example.spaceplan.domain.MeasurementUnit
import com.example.spaceplan.domain.Orientation
import com.example.spaceplan.domain.SpacePlanMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.math.BigDecimal

class PersistenceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() = scope.cancel()

    private fun dataStore(name: String) =
        PreferenceDataStoreFactory.create(scope = scope) { tmp.root.resolve("$name.preferences_pb") }

    private fun record(id: Long, equalGap: BigDecimal? = BigDecimal("66.66666666666666666666666666666667")) = HistoryRecord(
        id = id,
        timestampMillis = 1_700_000_000_000 + id,
        mode = SpacePlanMode.EVEN_DISTRIBUTION,
        unit = MeasurementUnit.INCH,
        availableMm = BigDecimal("1000"),
        objectMm = BigDecimal("200"),
        gapMm = BigDecimal("0"),
        objectCount = 4,
        edgeGapMode = EdgeGapMode.EQUAL,
        edgeGapMm = BigDecimal("2.5"),
        objectsFit = 4,
        usedMm = BigDecimal("1000"),
        remainingMm = BigDecimal("200"),
        equalGapMm = equalGap,
        doesNotFit = false,
    )

    // 25. History serialization
    @Test
    fun historyCodec_roundTrip() {
        val records = listOf(record(1), record(2, equalGap = null))
        assertEquals(records, HistoryCodec.decode(HistoryCodec.encode(records)))
    }

    @Test
    fun historyCodec_skipsMalformedLines() {
        val encoded = HistoryCodec.encode(listOf(record(1))) + "\ngarbage;line\n"
        assertEquals(1, HistoryCodec.decode(encoded).size)
    }

    @Test
    fun history_keepsNewest50() {
        var list = emptyList<HistoryRecord>()
        for (id in 1L..51L) list = HistoryCodec.addRecord(list, record(id))
        assertEquals(50, list.size)
        assertEquals(51L, list.first().id)
        assertTrue(list.none { it.id == 1L })
    }

    @Test
    fun historyRepository_persists() = runBlocking {
        val repo = HistoryRepository(dataStore("history"))
        repo.add(record(1))
        repo.add(record(2))
        assertEquals(listOf(2L, 1L), repo.history.first().map { it.id })
        repo.delete(2)
        assertEquals(listOf(1L), repo.history.first().map { it.id })
        repo.clear()
        assertTrue(repo.history.first().isEmpty())
    }

    // 26. Settings persistence
    @Test
    fun settingsRepository_persistsAndResets() = runBlocking {
        val repo = SettingsRepository(dataStore("settings"))
        assertEquals(AppSettings(), repo.settings.first())
        repo.update {
            it.copy(
                themeMode = ThemeMode.DARK,
                defaultMode = SpacePlanMode.EVEN_DISTRIBUTION,
                defaultUnit = MeasurementUnit.FOOT,
                defaultAvailable = "12.5",
                defaultObjectCount = 7,
                orientation = Orientation.VERTICAL,
                decimalPlaces = 3,
                showFormula = false,
                hapticFeedback = false,
            )
        }
        val saved = repo.settings.first()
        assertEquals(ThemeMode.DARK, saved.themeMode)
        assertEquals(SpacePlanMode.EVEN_DISTRIBUTION, saved.defaultMode)
        assertEquals(MeasurementUnit.FOOT, saved.defaultUnit)
        assertEquals("12.5", saved.defaultAvailable)
        assertEquals(7, saved.defaultObjectCount)
        assertEquals(Orientation.VERTICAL, saved.orientation)
        assertEquals(3, saved.decimalPlaces)
        assertEquals(false, saved.showFormula)
        assertEquals(false, saved.hapticFeedback)
        repo.reset()
        assertEquals(AppSettings(), repo.settings.first())
    }
}
