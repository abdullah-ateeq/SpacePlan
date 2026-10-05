package com.example.spaceplan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.spaceplan.SpacePlanApplication
import com.example.spaceplan.data.AppSettings
import com.example.spaceplan.data.HistoryRecord
import com.example.spaceplan.data.HistoryRepository
import com.example.spaceplan.data.SettingsRepository
import com.example.spaceplan.domain.EdgeGapMode
import com.example.spaceplan.domain.InputField
import com.example.spaceplan.domain.InputValidator
import com.example.spaceplan.domain.MeasurementFormatter
import com.example.spaceplan.domain.MeasurementUnit
import com.example.spaceplan.domain.NumberParser
import com.example.spaceplan.domain.Orientation
import com.example.spaceplan.domain.SpacePlanException
import com.example.spaceplan.domain.SpacePlanMode
import com.example.spaceplan.domain.SpacePlanResult
import com.example.spaceplan.domain.calculate
import com.example.spaceplan.domain.toMillimeters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * A length input. [exactMm] keeps the precise physical value after a unit change so that
 * converting back and forth never drifts; it is dropped as soon as the user edits the text.
 */
data class MeasureField(val text: String, val exactMm: BigDecimal? = null) {
    fun toMillimeters(unit: MeasurementUnit): BigDecimal? =
        exactMm ?: NumberParser.parseDecimal(text)?.toMillimeters(unit)
}

data class CalculatorInputs(
    val mode: SpacePlanMode,
    val unit: MeasurementUnit,
    val available: MeasureField,
    val objectSize: MeasureField,
    val gap: MeasureField,
    val edgeGap: MeasureField,
    val countText: String,
    val edgeGapMode: EdgeGapMode,
    val orientation: Orientation,
)

data class CalculatorUiState(
    val inputs: CalculatorInputs,
    val errors: Map<InputField, String> = emptyMap(),
    val result: SpacePlanResult? = null,
    val calculationError: String? = null,
)

class SpacePlanViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val history: StateFlow<List<HistoryRecord>> = historyRepository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val inputs = MutableStateFlow(defaultInputs(AppSettings()))

    val uiState: StateFlow<CalculatorUiState> = inputs.map(::evaluate)
        .stateIn(viewModelScope, SharingStarted.Eagerly, evaluate(inputs.value))

    init {
        viewModelScope.launch {
            val stored = settingsRepository.settings.first()
            inputs.value = defaultInputs(stored)
            _settingsLoaded.value = true
        }
    }

    // region Calculator input

    fun setMode(mode: SpacePlanMode) = inputs.update { it.copy(mode = mode) }

    fun setAvailable(text: String) = inputs.update { it.copy(available = MeasureField(text)) }

    fun setObjectSize(text: String) = inputs.update { it.copy(objectSize = MeasureField(text)) }

    fun setGap(text: String) = inputs.update { it.copy(gap = MeasureField(text)) }

    fun setEdgeGap(text: String) = inputs.update { it.copy(edgeGap = MeasureField(text)) }

    fun setCount(text: String) = inputs.update { it.copy(countText = text.filter(Char::isDigit).take(4)) }

    fun setEdgeGapMode(mode: EdgeGapMode) = inputs.update { it.copy(edgeGapMode = mode) }

    fun setOrientation(orientation: Orientation) = inputs.update { it.copy(orientation = orientation) }

    /** Changes the unit while preserving the physical value of every length field. */
    fun setUnit(unit: MeasurementUnit) = inputs.update { current ->
        if (current.unit == unit) return@update current
        val decimals = settings.value.decimalPlaces
        fun MeasureField.convert(): MeasureField {
            val mm = toMillimeters(current.unit) ?: return this
            return MeasureField(MeasurementFormatter.formatInputValue(mm, unit, decimals), mm)
        }
        current.copy(
            unit = unit,
            available = current.available.convert(),
            objectSize = current.objectSize.convert(),
            gap = current.gap.convert(),
            edgeGap = current.edgeGap.convert(),
        )
    }

    fun reset() {
        inputs.value = defaultInputs(settings.value)
    }

    // endregion

    // region History

    /** Saves the current valid result. Returns false when there is nothing valid to save. */
    fun saveCurrent(): Boolean {
        val state = uiState.value
        val result = state.result ?: return false
        val current = state.inputs
        val unit = current.unit
        val now = System.currentTimeMillis()
        val record = HistoryRecord(
            id = now,
            timestampMillis = now,
            mode = current.mode,
            unit = unit,
            availableMm = result.availableMm,
            objectMm = result.objectMm,
            gapMm = current.gap.toMillimeters(unit)?.takeIf { it.signum() >= 0 } ?: BigDecimal.ZERO,
            objectCount = NumberParser.parseCount(current.countText) ?: settings.value.defaultObjectCount,
            edgeGapMode = current.edgeGapMode,
            edgeGapMm = current.edgeGap.toMillimeters(unit)?.takeIf { it.signum() >= 0 } ?: BigDecimal.ZERO,
            objectsFit = result.objectsFit,
            usedMm = result.usedSpaceMm,
            remainingMm = result.remainingSpaceMm,
            equalGapMm = result.equalGapMm,
            doesNotFit = result.doesNotFit,
        )
        viewModelScope.launch { historyRepository.add(record) }
        return true
    }

    fun restore(record: HistoryRecord) {
        fun field(mm: BigDecimal) = MeasureField(MeasurementFormatter.formatInputValue(mm, record.unit, 0), mm)
        inputs.update {
            it.copy(
                mode = record.mode,
                unit = record.unit,
                available = field(record.availableMm),
                objectSize = field(record.objectMm),
                gap = field(record.gapMm),
                edgeGap = field(record.edgeGapMm),
                countText = record.objectCount.toString(),
                edgeGapMode = record.edgeGapMode,
            )
        }
    }

    fun deleteHistory(id: Long) = viewModelScope.launch { historyRepository.delete(id) }

    fun clearHistory() = viewModelScope.launch { historyRepository.clear() }

    // endregion

    // region Settings

    fun updateSettings(transform: (AppSettings) -> AppSettings) =
        viewModelScope.launch { settingsRepository.update(transform) }

    fun resetSettings() = viewModelScope.launch { settingsRepository.reset() }

    // endregion

    private fun evaluate(current: CalculatorInputs): CalculatorUiState {
        val unit = current.unit
        val validation = InputValidator.validate(
            mode = current.mode,
            unit = unit,
            availableMm = current.available.toMillimeters(unit),
            objectMm = current.objectSize.toMillimeters(unit),
            gapMm = current.gap.toMillimeters(unit),
            count = NumberParser.parseCount(current.countText),
            edgeGapMode = current.edgeGapMode,
            edgeGapMm = current.edgeGap.toMillimeters(unit),
        )
        val input = validation.input ?: return CalculatorUiState(current, validation.errors)
        return try {
            CalculatorUiState(current, result = input.calculate())
        } catch (e: SpacePlanException) {
            CalculatorUiState(current, calculationError = e.message)
        }
    }

    companion object {
        fun defaultInputs(s: AppSettings) = CalculatorInputs(
            mode = s.defaultMode,
            unit = s.defaultUnit,
            available = MeasureField(s.defaultAvailable),
            objectSize = MeasureField(s.defaultObjectSize),
            gap = MeasureField(s.defaultGap),
            edgeGap = MeasureField(s.defaultGap),
            countText = s.defaultObjectCount.toString(),
            edgeGapMode = EdgeGapMode.NONE,
            orientation = s.orientation,
        )

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SpacePlanApplication
                SpacePlanViewModel(app.container.settingsRepository, app.container.historyRepository)
            }
        }
    }
}
