package com.uzuu.letplay.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uzuu.letplay.navigation.AppNavigator
import com.uzuu.letplay.navigation.AppRoute
import com.uzuu.letplay.navigation.NoOpAppNavigator
import com.uzuu.letplay.resources.R
import com.uzuu.letplay.resources.theme.BaseTheme

enum class MainTab(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
) {
    Camera(R.string.tab_camera, R.drawable.ic_tab_camera),
    Rooms(R.string.tab_rooms, R.drawable.ic_tab_rooms),
    Memories(R.string.tab_memories, R.drawable.ic_tab_memories),
    Messages(R.string.tab_messages, R.drawable.ic_tab_messages)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navigator: AppNavigator = NoOpAppNavigator(),
    initialTab: MainTab = MainTab.Camera,
    onTabSelected: (MainTab) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            MainBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelect = { tab ->
                    selectedTab = tab
                    onTabSelected(tab)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                MainTab.Camera -> CameraTabPlaceholder(
                    onOpenRoom = { roomId -> navigator.navigate(AppRoute.RoomDetail(roomId)) }
                )
                MainTab.Rooms -> RoomsTabPlaceholder(
                    onOpenRoom = { roomId -> navigator.navigate(AppRoute.RoomDetail(roomId)) }
                )
                MainTab.Memories -> MemoriesTabPlaceholder()
                MainTab.Messages -> MessagesTabPlaceholder()
            }
        }
    }
}

@Composable
fun MainBottomNavigationBar(
    selectedTab: MainTab,
    onTabSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val title = stringResource(id = tab.titleRes)

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelect(tab) },
                icon = {
                    Icon(
                        painter = painterResource(id = tab.iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun CameraTabPlaceholder(
    onOpenRoom: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tab_camera),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(id = R.string.placeholder_camera_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.placeholder_camera_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onOpenRoom("demo-room-01") },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Mở phòng demo")
        }
    }
}

@Composable
fun RoomsTabPlaceholder(
    onOpenRoom: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sampleRooms = listOf(
        Pair("keo-da-bong-01", "Kèo đá bóng thứ 6"),
        Pair("keo-ca-phe-02", "Cà phê cuối tuần"),
        Pair("keo-an-toi-03", "Tiệc sinh nhật nhóm")
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.placeholder_rooms_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(id = R.string.placeholder_rooms_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        items(sampleRooms) { (roomId, roomName) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenRoom(roomId) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tab_rooms),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = roomName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mã: $roomId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemoriesTabPlaceholder(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tab_memories),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(id = R.string.placeholder_memories_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.placeholder_memories_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MessagesTabPlaceholder(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tab_messages),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(id = R.string.placeholder_messages_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.placeholder_messages_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BaseTheme {
        MainScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun CameraTabPreview() {
    BaseTheme {
        CameraTabPlaceholder()
    }
}

@Preview(showBackground = true)
@Composable
fun RoomsTabPreview() {
    BaseTheme {
        RoomsTabPlaceholder()
    }
}

@Preview(showBackground = true)
@Composable
fun MemoriesTabPreview() {
    BaseTheme {
        MemoriesTabPlaceholder()
    }
}

@Preview(showBackground = true)
@Composable
fun MessagesTabPreview() {
    BaseTheme {
        MessagesTabPlaceholder()
    }
}
