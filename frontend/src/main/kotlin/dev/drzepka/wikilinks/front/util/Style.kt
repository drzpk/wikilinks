package dev.drzepka.wikilinks.front.util

import kotlin.math.floor

fun getFillColorForLevel(level: Int, levelCount: Int): String = getColorForLevel(level, levelCount, 40)
fun getBorderColorForLevel(level: Int, levelCount: Int): String = getColorForLevel(level, levelCount, 20)

private fun getColorForLevel(level: Int, levelCount: Int, hslLevel: Int): String {
    val step = 360.0 / levelCount
    val degrees = floor(level * step + 0.5).toInt()
    return "hsl(${degrees}deg 100% ${hslLevel}%)"
}
