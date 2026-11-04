package com.example.util

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AssessmentSubtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false
)

object SubtaskChecklistHelper {

    fun parseSubtasks(json: String): List<AssessmentSubtask> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val title = obj.optString("title").trim()
                    if (title.isNotEmpty()) {
                        add(
                            AssessmentSubtask(
                                id = obj.optString("id").ifBlank { "sub_$i" },
                                title = title,
                                isDone = obj.optBoolean("done", false)
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun serializeSubtasks(items: List<AssessmentSubtask>): String {
        if (items.isEmpty()) return ""
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("done", item.isDone)
                }
            )
        }
        return arr.toString()
    }

    fun generateDefaultSubtasksForType(type: String): List<AssessmentSubtask> {
        val templates = when (type) {
            "MID_SEM", "FINAL_EXAM", "CLASS_TEST", "QUIZ" -> listOf(
                "Review all lecture slides & key formulas",
                "Complete active-recall flashcards",
                "Solve timed practice quiz / past questions",
                "Pack exam hall materials & ID card"
            )
            "PROJECT", "PRESENTATION" -> listOf(
                "Define scope & milestone outline",
                "Gather academic sources & citations",
                "Build core deliverable / slide deck",
                "Rehearse & submit final deliverable"
            )
            else -> listOf(
                "Read prompt & rubric requirements",
                "Research & format academic citations",
                "Write complete first draft",
                "Proofread & submit before deadline"
            )
        }
        return templates.map { AssessmentSubtask(title = it, isDone = false) }
    }
}
