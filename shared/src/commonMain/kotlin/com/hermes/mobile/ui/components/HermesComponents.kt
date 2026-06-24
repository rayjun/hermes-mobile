package com.hermes.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.mobile.models.RiskLevel
import com.hermes.mobile.ui.ApprovalCardState
import com.hermes.mobile.ui.theme.HermesColors
import com.hermes.mobile.ui.theme.HermesRadius
import com.hermes.mobile.ui.theme.HermesSpacing
import com.hermes.mobile.ui.theme.HermesTheme
import com.hermes.mobile.ui.theme.HermesTypography

@Composable
fun HermesSectionHeader(
    state: SectionHeaderState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HermesSpacing.ScreenHorizontal.dp, vertical = 6.dp),
    ) {
        BasicText(
            text = "■",
            style = sectionLabelStyle().copy(color = color(HermesTheme.colors.accent)),
        )
        Spacer(Modifier.width(6.dp))
        BasicText(
            text = sectionHeaderLabel(state.title, state.count),
            style = sectionLabelStyle().copy(color = color(HermesTheme.colors.textSecondary)),
        )
    }
}

@Composable
fun HermesInboxItem(
    state: InboxItemState,
    modifier: Modifier = Modifier,
) {
    val style = InboxItemStyle.forKind(state.kind)
    val shape = RoundedCornerShape(HermesRadius.Card.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HermesSpacing.ScreenHorizontal.dp, vertical = 4.dp)
            .clip(shape)
            .background(color(HermesTheme.colors.surface))
            .border(1.dp, color(HermesTheme.colors.border), shape)
            .heightIn(min = HermesSpacing.RowMinHeight.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        BasicText(
            text = state.risk?.let { ApprovalRiskStyle.forRisk(it).label } ?: style.marker,
            style = badgeStyle().copy(color = color(state.risk?.let { ApprovalRiskStyle.forRisk(it).foreground } ?: style.foreground)),
        )
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = state.title,
                style = rowTitleStyle().copy(color = color(HermesTheme.colors.textPrimary)),
            )
            BasicText(
                text = state.subtitle,
                style = rowSubtitleStyle().copy(color = color(HermesTheme.colors.textSecondary)),
            )
        }
    }
}

@Composable
fun HermesApprovalCard(
    state: ApprovalCardState,
    modifier: Modifier = Modifier,
    onApprove: (() -> Unit)? = null,
    onDeny: (() -> Unit)? = null,
) {
    val riskStyle = ApprovalRiskStyle.forRisk(state.risk)
    val shape = RoundedCornerShape(HermesRadius.Card.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HermesSpacing.ScreenHorizontal.dp, vertical = 6.dp)
            .clip(shape)
            .background(color(HermesTheme.colors.surface))
            .border(1.dp, color(HermesTheme.colors.border), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(
            text = riskStyle.label,
            style = badgeStyle().copy(color = color(riskStyle.foreground)),
        )
        BasicText(
            text = state.title,
            style = rowTitleStyle().copy(color = color(HermesTheme.colors.textPrimary)),
        )
        BasicText(
            text = state.summary,
            style = rowSubtitleStyle().copy(color = color(HermesTheme.colors.textSecondary)),
        )
        state.contextRows.forEach { (label, value) ->
            Row {
                BasicText(
                    text = label,
                    style = monoStyle().copy(color = color(HermesTheme.colors.textTertiary)),
                    modifier = Modifier.width(72.dp),
                )
                BasicText(
                    text = value,
                    style = monoStyle().copy(color = color(HermesTheme.colors.textPrimary)),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ApprovalActionButton(
                text = "Approve",
                foreground = HermesColors.Success,
                onClick = onApprove,
            )
            ApprovalActionButton(
                text = "Deny",
                foreground = HermesColors.Error,
                onClick = onDeny,
            )
        }
    }
}

@Composable
private fun ApprovalActionButton(
    text: String,
    foreground: Long,
    onClick: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(HermesRadius.Button.dp)
    val modifier = Modifier
        .clip(shape)
        .border(1.dp, color(HermesTheme.colors.border), shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 10.dp, vertical = 6.dp)
    BasicText(
        text = text,
        style = badgeStyle().copy(color = color(foreground)),
        modifier = modifier,
    )
}

@Composable
fun HermesCommandBar(
    state: CommandBarState,
    modifier: Modifier = Modifier,
    onTextChange: ((String) -> Unit)? = null,
    onSend: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(HermesRadius.Input.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HermesSpacing.ScreenHorizontal.dp, vertical = 8.dp)
            .clip(shape)
            .background(color(HermesTheme.colors.surface))
            .border(1.dp, color(HermesTheme.colors.border), shape)
            .heightIn(min = HermesSpacing.CommandBarHeight.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        BasicText(
            text = "+",
            style = rowTitleStyle().copy(color = color(HermesTheme.colors.accent)),
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = state.text,
            onValueChange = { onTextChange?.invoke(it) },
            enabled = onTextChange != null,
            textStyle = rowTitleStyle().copy(color = color(HermesTheme.colors.textPrimary)),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (state.text.isEmpty()) {
                    BasicText(
                        text = state.placeholder,
                        style = rowTitleStyle().copy(color = color(HermesTheme.colors.textTertiary)),
                    )
                }
                innerTextField()
            },
        )
        Spacer(Modifier.width(10.dp))
        BasicText(
            text = "Send",
            style = badgeStyle().copy(
                color = color(if (state.canSend) HermesTheme.colors.accent else HermesTheme.colors.textTertiary),
            ),
            modifier = Modifier.then(if (state.canSend && onSend != null) Modifier.clickable(onClick = onSend) else Modifier),
        )
    }
}

@Composable
private fun sectionLabelStyle(): TextStyle = TextStyle(
    fontSize = HermesTypography.SectionLabel.sizeSp.sp,
    fontWeight = FontWeight(HermesTypography.SectionLabel.weight),
    lineHeight = HermesTypography.SectionLabel.lineHeightSp.sp,
)

@Composable
private fun rowTitleStyle(): TextStyle = TextStyle(
    fontSize = HermesTypography.RowTitle.sizeSp.sp,
    fontWeight = FontWeight(HermesTypography.RowTitle.weight),
    lineHeight = HermesTypography.RowTitle.lineHeightSp.sp,
)

@Composable
private fun rowSubtitleStyle(): TextStyle = TextStyle(
    fontSize = HermesTypography.RowSubtitle.sizeSp.sp,
    fontWeight = FontWeight(HermesTypography.RowSubtitle.weight),
    lineHeight = HermesTypography.RowSubtitle.lineHeightSp.sp,
)

@Composable
private fun monoStyle(): TextStyle = TextStyle(
    fontSize = HermesTypography.MonoSmall.sizeSp.sp,
    fontWeight = FontWeight(HermesTypography.MonoSmall.weight),
    lineHeight = HermesTypography.MonoSmall.lineHeightSp.sp,
    fontFamily = FontFamily.Monospace,
)

@Composable
private fun badgeStyle(): TextStyle = TextStyle(
    fontSize = HermesTypography.Badge.sizeSp.sp,
    fontWeight = FontWeight(HermesTypography.Badge.weight),
    lineHeight = HermesTypography.Badge.lineHeightSp.sp,
)

private fun color(argb: Long): Color = Color(argb.toULong())

@Suppress("unused")
private fun RiskLevel.badgeLabel(): String = ApprovalRiskStyle.forRisk(this).label
