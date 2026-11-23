package com.example.domain.model

enum class DhikrCategory(val id: String, val arabicTitle: String) {
  MORNING("morning", "أذكار الصباح"),
  EVENING("evening", "أذكار المساء"),
  AFTER_PRAYER("after_prayer", "بعد الصلاة"),
}

data class DhikrItem(
  val id: String,
  val category: DhikrCategory,
  val text: String,
  val reference: String,
  val targetCount: Int,
)

data class TasbihPhrase(
  val id: String,
  val text: String,
  val virtue: String,
)
