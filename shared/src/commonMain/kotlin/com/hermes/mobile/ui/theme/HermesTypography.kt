package com.hermes.mobile.ui.theme

data class HermesTextStyle(
    val sizeSp: Int,
    val weight: Int,
    val lineHeightSp: Int,
)

object HermesTypography {
    val ScreenTitle = HermesTextStyle(sizeSp = 16, weight = 600, lineHeightSp = 20)
    val SectionLabel = HermesTextStyle(sizeSp = 10, weight = 600, lineHeightSp = 12)
    val RowTitle = HermesTextStyle(sizeSp = 13, weight = 500, lineHeightSp = 18)
    val RowSubtitle = HermesTextStyle(sizeSp = 11, weight = 400, lineHeightSp = 16)
    val Body = HermesTextStyle(sizeSp = 14, weight = 400, lineHeightSp = 20)
    val BodySmall = HermesTextStyle(sizeSp = 12, weight = 400, lineHeightSp = 16)
    val Mono = HermesTextStyle(sizeSp = 12, weight = 400, lineHeightSp = 16)
    val MonoSmall = HermesTextStyle(sizeSp = 11, weight = 400, lineHeightSp = 16)
    val Badge = HermesTextStyle(sizeSp = 9, weight = 700, lineHeightSp = 12)
}
