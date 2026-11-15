package com.uzuu.letplay.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uzuu.letplay.domain.model.User
import com.uzuu.letplay.navigation.AppNavigator
import com.uzuu.letplay.navigation.AppRoute
import com.uzuu.letplay.resources.R
import com.uzuu.letplay.ui.base.DefaultUiErrorMessageMapper

@Composable
fun UsersScreen(
    viewModel: UsersViewModel,
    modifier: Modifier = Modifier,
    navigator: AppNavigator
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val current = state) {
        UsersUiState.Loading -> Text(stringResource(R.string.loading_users), modifier.padding(16.dp))
        is UsersUiState.Error -> Column(modifier.padding(16.dp)) {
            val context = LocalContext.current
            val errorMessageMapper = remember(context) { DefaultUiErrorMessageMapper(context) }
            val errorMessage = remember(current.error, context) {
                errorMessageMapper.map(context, current.error)
            }
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            Button(onClick = viewModel::loadUsers) { Text(stringResource(R.string.button_retry)) }
        }
        is UsersUiState.Success -> Column(modifier.fillMaxSize()) {
            Button(
                onClick = { navigator.navigate(AppRoute.Todo) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.button_open_todo))
            }
            UsersList(current.users, Modifier.weight(1f))
            if (current.hasNext) {
                Button(
                    onClick = viewModel::loadNextPage,
                    enabled = !current.isLoadingMore,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(if (current.isLoadingMore) stringResource(R.string.loading_more) else stringResource(R.string.button_load_more))
                }
            }
        }
    }
}

@Composable
private fun UsersList(users: List<User>, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users, key = { it.id }) { user ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(user.name, style = MaterialTheme.typography.titleMedium)
                    Text("@${user.username}", style = MaterialTheme.typography.bodyMedium)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium)
                    user.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
