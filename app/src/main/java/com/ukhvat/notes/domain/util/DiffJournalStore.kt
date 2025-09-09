package com.ukhvat.notes.domain.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized edit journal per note to build precise diffs for versions.
 * Stores coalesced operations: insert/delete/replace with ranges and timestamps.
 */
object DiffJournalStore {
    data class EditOp(
        val type: String, // "insert" | "delete" | "replace"
        val start: Int,
        val end: Int,
        val text: String,
        val ts: Long
    )

    private val noteIdToOps: MutableMap<Long, MutableList<EditOp>> = ConcurrentHashMap()
    private val noteIdToLastOpTime: MutableMap<Long, Long> = ConcurrentHashMap()

    fun recordInsert(noteId: Long, start: Int, text: String) {
        record(noteId, EditOp("insert", start, start + text.length, text, System.currentTimeMillis()))
    }

    fun recordDelete(noteId: Long, start: Int, end: Int) {
        record(noteId, EditOp("delete", start, end, "", System.currentTimeMillis()))
    }

    fun recordReplace(noteId: Long, start: Int, end: Int, text: String) {
        record(noteId, EditOp("replace", start, end, text, System.currentTimeMillis()))
    }

    @Synchronized
    private fun record(noteId: Long, op: EditOp) {
        val list = noteIdToOps.getOrPut(noteId) { mutableListOf() }
        val lastTime = noteIdToLastOpTime[noteId] ?: 0L
        val now = op.ts
        val last = list.lastOrNull()
        if (last != null && now - lastTime < 500 && last.type == op.type) {
            val merged: EditOp? = when (op.type) {
                "insert" -> if (op.start == last.end) last.copy(end = op.end, text = last.text + op.text, ts = now) else null
                "delete" -> if (op.end == last.start) last.copy(start = op.start, ts = now) else null
                else -> null
            }
            if (merged != null) {
                list[list.lastIndex] = merged
                noteIdToLastOpTime[noteId] = now
                return
            }
        }
        list.add(op)
        noteIdToLastOpTime[noteId] = now
    }

    @Synchronized
    fun snapshotAndClear(noteId: Long): String? {
        val list = noteIdToOps[noteId] ?: return null
        if (list.isEmpty()) return null
        val json = buildString {
            append('[')
            list.forEachIndexed { idx, op ->
                if (idx > 0) append(',')
                append('{')
                append("\"type\":\"").append(op.type).append("\",")
                append("\"start\":").append(op.start).append(',')
                append("\"end\":").append(op.end).append(',')
                append("\"text\":\"")
                    .append(op.text.replace("\\", "\\\\").replace("\"", "\\\""))
                    .append("\",")
                append("\"ts\":").append(op.ts)
                append('}')
            }
            append(']')
        }
        list.clear()
        noteIdToLastOpTime.remove(noteId)
        return json
    }
}


