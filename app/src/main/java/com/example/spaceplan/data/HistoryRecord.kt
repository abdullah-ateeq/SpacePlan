package com.example.spaceplan.data

import com.example.spaceplan.domain.EdgeGapMode
import com.example.spaceplan.domain.MeasurementUnit
import com.example.spaceplan.domain.SpacePlanMode
import java.math.BigDecimal

/** A saved calculation. Lengths are stored in millimeters; [unit] is the unit it was entered in. */
data class HistoryRecord(
    val id: Long,
    val timestampMillis: Long,
    val mode: SpacePlanMode,
    val unit: MeasurementUnit,
    val availableMm: BigDecimal,
    val objectMm: BigDecimal,
    val gapMm: BigDecimal,
    val objectCount: Int,
    val edgeGapMode: EdgeGapMode,
    val edgeGapMm: BigDecimal,
    val objectsFit: Int,
    val usedMm: BigDecimal,
    val remainingMm: BigDecimal,
    val equalGapMm: BigDecimal?,
    val doesNotFit: Boolean,
)

/** Compact line-based encoding for storing history in a single Preferences string. */
object HistoryCodec {
    const val MAX_RECORDS = 50
    private const val VERSION = "v1"
    private const val SEP = ";"

    fun encode(records: List<HistoryRecord>): String = records.joinToString("\n") { r ->
        listOf(
            VERSION,
            r.id.toString(),
            r.timestampMillis.toString(),
            r.mode.name,
            r.unit.name,
            r.availableMm.toPlainString(),
            r.objectMm.toPlainString(),
            r.gapMm.toPlainString(),
            r.objectCount.toString(),
            r.edgeGapMode.name,
            r.edgeGapMm.toPlainString(),
            r.objectsFit.toString(),
            r.usedMm.toPlainString(),
            r.remainingMm.toPlainString(),
            r.equalGapMm?.toPlainString().orEmpty(),
            r.doesNotFit.toString(),
        ).joinToString(SEP)
    }

    /** Decodes stored history, silently skipping any malformed lines. */
    fun decode(encoded: String): List<HistoryRecord> =
        encoded.lineSequence().filter { it.isNotBlank() }.mapNotNull(::decodeLine).toList()

    private fun decodeLine(line: String): HistoryRecord? = try {
        val f = line.split(SEP)
        if (f.size != 16 || f[0] != VERSION) {
            null
        } else {
            HistoryRecord(
                id = f[1].toLong(),
                timestampMillis = f[2].toLong(),
                mode = SpacePlanMode.valueOf(f[3]),
                unit = MeasurementUnit.valueOf(f[4]),
                availableMm = BigDecimal(f[5]),
                objectMm = BigDecimal(f[6]),
                gapMm = BigDecimal(f[7]),
                objectCount = f[8].toInt(),
                edgeGapMode = EdgeGapMode.valueOf(f[9]),
                edgeGapMm = BigDecimal(f[10]),
                objectsFit = f[11].toInt(),
                usedMm = BigDecimal(f[12]),
                remainingMm = BigDecimal(f[13]),
                equalGapMm = f[14].takeIf { it.isNotEmpty() }?.let(::BigDecimal),
                doesNotFit = f[15].toBooleanStrict(),
            )
        }
    } catch (_: IllegalArgumentException) {
        null
    }

    /** Newest first; drops the oldest records beyond [max]. */
    fun addRecord(existing: List<HistoryRecord>, record: HistoryRecord, max: Int = MAX_RECORDS): List<HistoryRecord> =
        (listOf(record) + existing.filter { it.id != record.id }).take(max)
}
