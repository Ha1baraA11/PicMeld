package com.ha1baraa11.picmeld

data class LayoutConfig(
    val columns: Int,
    val rows: Int,
    val groupSize: Int,
    val label: String,
    val filenameTag: String
) {
    companion object {
        val LAYOUT_2X2 = LayoutConfig(2, 2, 4, "2x2 方格", "2X2")
        val LAYOUT_3X3 = LayoutConfig(3, 3, 9, "3x3 方格", "3X3")
        val LAYOUT_1X3 = LayoutConfig(1, 3, 3, "1x3 竖条", "1X3")
        val ALL = listOf(LAYOUT_2X2, LAYOUT_3X3, LAYOUT_1X3)
    }
}
