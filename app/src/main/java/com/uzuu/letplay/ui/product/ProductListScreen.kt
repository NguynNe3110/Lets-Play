package com.uzuu.letplay.ui.product

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uzuu.letplay.domain.model.Product
import com.uzuu.letplay.navigation.AppNavigator
import com.uzuu.letplay.navigation.NoOpAppNavigator
import com.uzuu.letplay.resources.R
import com.uzuu.letplay.ui.base.BaseScreen
import com.uzuu.letplay.ui.base.DefaultUiErrorMessageMapper
import com.uzuu.letplay.ui.components.loading.ProductCardSkeleton
import com.uzuu.letplay.ui.components.loading.rememberShimmerBrush

/**
 * Screen presenting the public product catalog list/grid (REST-2 & P9.1).
 *
 * Implements:
 * - Integration with [BaseScreen] and [ProductViewModel].
 * - Skeleton placeholders during initial load with native shimmer animations (P9.1).
 * - Non-destructive pull-to-refresh / explicit refresh preserving existing items and scroll position.
 * - Auto-prefetch pagination on scroll near end, preserving items on append.
 * - Inline append error footer with retry button preserving loaded products.
 * - Search bar with instant text feedback and debounced query execution.
 * - Category filter chips and sorting options resetting pagination to page 1.
 * - Full-screen initial error view and empty state view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = remember { NoOpAppNavigator() },
    onProductClick: (Product) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val errorMessageMapper = remember(context) { DefaultUiErrorMessageMapper(context) }

    BaseScreen(
        viewModel = viewModel,
        modifier = modifier,
        navigator = navigator,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.products_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !uiState.isRefreshing && !uiState.isLoadingInitial
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.button_retry)
                        )
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Refresh indicator bar (preserves existing items below)
            if (uiState.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Search text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onQueryChanged,
                placeholder = { Text(stringResource(R.string.search_products_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category filter bar
            if (uiState.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text(stringResource(R.string.all_categories)) }
                    )

                    uiState.categories.forEach { category ->
                        FilterChip(
                            selected = uiState.selectedCategory?.id == category.id,
                            onClick = {
                                if (uiState.selectedCategory?.id == category.id) {
                                    viewModel.setCategory(null)
                                } else {
                                    viewModel.setCategory(category)
                                }
                            },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            // Sort options bar
            val sortOptions = listOf(
                "-createdAt" to stringResource(R.string.sort_newest),
                "createdAt" to stringResource(R.string.sort_oldest),
                "price" to stringResource(R.string.sort_price_low_high),
                "-price" to stringResource(R.string.sort_price_high_low),
                "name" to stringResource(R.string.sort_name_az)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                sortOptions.forEach { (sortKey, label) ->
                    val isSelected = uiState.filter.sort == sortKey ||
                        (uiState.filter.sort == null && sortKey == "-createdAt")
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSort(sortKey) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Body content
            when {
                // P9.1 Skeleton initial loading
                uiState.isLoadingInitial -> {
                    val shimmerBrush = rememberShimmerBrush()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(6) {
                            ProductCardSkeleton(brush = shimmerBrush)
                        }
                    }
                }

                // Initial load error with retry button
                uiState.initialError != null -> {
                    val errorMessage = remember(uiState.initialError, context) {
                        uiState.initialError?.let { err ->
                            try {
                                errorMessageMapper.map(context, err)
                            } catch (_: Exception) {
                                err.message ?: context.getString(R.string.error_server_generic)
                            }
                        } ?: context.getString(R.string.error_server_generic)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::retryInitialLoad) {
                            Text(stringResource(R.string.button_retry))
                        }
                    }
                }

                // Empty catalog state
                uiState.isEmpty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_products_list),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        if (uiState.filter.query != null || uiState.selectedCategory != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = {
                                viewModel.onQueryChanged("")
                                viewModel.setCategory(null)
                            }) {
                                Text(stringResource(R.string.button_dismiss))
                            }
                        }
                    }
                }

                // Products list / grid
                else -> {
                    val gridState = rememberLazyGridState()

                    // Auto-prefetch pagination on scroll near bottom (P9)
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val totalItemsCount = gridState.layoutInfo.totalItemsCount
                            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 2
                        }
                    }

                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore && uiState.hasNextPage && !uiState.isAppending && !uiState.isRefreshing) {
                            viewModel.loadNextPage()
                        }
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.products,
                            key = { it.id }
                        ) { product ->
                            ProductItemCard(
                                product = product,
                                onClick = { onProductClick(product) }
                            )
                        }

                        // Append loading footer
                        if (uiState.isAppending) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        // Append failure retry footer
                        if (uiState.appendError != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.load_more_failed),
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = viewModel::retryLoadNextPage) {
                                            Text(stringResource(R.string.button_retry))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
