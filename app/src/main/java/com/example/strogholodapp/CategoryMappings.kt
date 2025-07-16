package com.example.strogholodapp

val categoriesMap = mapOf(
    "Бонеты" to "Bonety",
    "Лари" to "Lari",
    "Витрины" to "Vitriny",
    "Горки встроенный холод" to "Gorki_vstroennyj",
    "Горки выносной холод" to "Gorki_vynosnoj",
    "Шкафы двухдверные" to "Shkafy_dvuhdvernye",
    "Шкафы однодверные" to "Shkafy_odnodvernye",
    "Кассы" to "Kassy",
    "Кухонное оборудование" to "Kuhonnoe_oborudovanie",
    "Стеллажи" to "Stellazhi"
)

fun humanCategory(code: String): String = categoriesMap.entries.find { it.value == code }?.key ?: "Бонеты"
