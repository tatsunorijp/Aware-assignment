// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.userlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.awarechat_android.designsystem.components.ConnectionStatusState
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme
import org.junit.Rule
import org.junit.Test

class UserListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun connectionFailureDisplaysBannerWithoutReplacingLocalSections() {
        composeRule.setContent {
            AwareChatAndroidTheme {
                UserListScreen(
                    state = UserListUiState(
                        screen = UserListScreenState.Ready(emptyList()),
                        discovery = UserDiscoveryState.Empty,
                        connection = ConnectionStatusState.Offline(
                            "Something went wrong. Please try again.",
                        ),
                    ),
                    onRefresh = {},
                    onRetryLocalLoad = {},
                    onRetryConnection = {},
                    onSelectUser = {},
                )
            }
        }

        composeRule.onNodeWithText("Something went wrong. Please try again.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("People on server").assertIsDisplayed()
    }
}
// MARK: - AI Generated - End
