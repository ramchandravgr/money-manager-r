package com.mmr.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.theme.MmrBrandColors

/**
 * Design 12 full-screen detail shell (see `docs/design/detail-screen-mockups` theme **12**):
 * brand top bar (back + title; optional overflow actions), gray [MmrBrandColors.ListBackground] body,
 * scrollable content, sticky [MmrFooterCtaBar]. Body primitives: [MmrDetailScreenForm].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrDetailScreenScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    useCompactHeaderTitle: Boolean = false,
    topBarActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    footerPrimaryLabel: String? = null,
    onFooterPrimary: (() -> Unit)? = null,
    footerPrimaryEnabled: Boolean = true,
    footerPrimaryTestTag: String? = null,
    footerSecondaryLabel: String? = null,
    onFooterSecondary: (() -> Unit)? = null,
    footerSecondaryEnabled: Boolean = true,
    footerSecondaryTestTag: String? = null,
    footerTextLabel: String? = null,
    onFooterText: (() -> Unit)? = null,
    footerTextEnabled: Boolean = true,
    footerTextIsDestructive: Boolean = false,
    footerTextTestTag: String? = null,
    isLoading: Boolean = false,
    isBlocking: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val horizontalPadding = mmrScreenHorizontalPadding()
    val hasFooter =
        (footerPrimaryLabel != null && onFooterPrimary != null) ||
            (footerSecondaryLabel != null && onFooterSecondary != null) ||
            (footerTextLabel != null && onFooterText != null)

    Scaffold(
        modifier = modifier,
        containerColor = MmrBrandColors.ListBackground,
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            MmrTopAppBar(
                title = title,
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useBrandHeaderStyle = true,
                useCompactHeaderTitle = useCompactHeaderTitle,
                actions = topBarActions,
            )
        },
        bottomBar = {
            if (hasFooter) {
                MmrFooterCtaBar(
                    primaryLabel = footerPrimaryLabel,
                    onPrimary = onFooterPrimary,
                    primaryEnabled = footerPrimaryEnabled && !isLoading && !isBlocking,
                    primaryTestTag = footerPrimaryTestTag,
                    secondaryLabel = footerSecondaryLabel,
                    onSecondary = onFooterSecondary,
                    secondaryEnabled = footerSecondaryEnabled && !isLoading && !isBlocking,
                    secondaryTestTag = footerSecondaryTestTag,
                    textLabel = footerTextLabel,
                    onText = onFooterText,
                    textEnabled = footerTextEnabled && !isLoading && !isBlocking,
                    textIsDestructive = footerTextIsDestructive,
                    textTestTag = footerTextTestTag,
                )
            }
        },
    ) { innerPadding ->
        when {
            isLoading || isBlocking ->
                MmrLoadingState(Modifier.fillMaxSize().padding(innerPadding))
            errorMessage != null ->
                MmrErrorState(
                    message = errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            else ->
                MmrAdaptiveWidthBox(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                start = horizontalPadding,
                                end = horizontalPadding,
                                top = 8.dp,
                                bottom = if (hasFooter) 8.dp else 16.dp,
                            ),
                        verticalArrangement =
                            androidx.compose.foundation.layout.Arrangement
                                .spacedBy(12.dp),
                        content = content,
                    )
                }
        }
    }
}
