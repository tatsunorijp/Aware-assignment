package com.example.awarechat_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.awarechat_android.app.AwareChatApp
import com.example.awarechat_android.app.AwareChatApplication
import com.example.awarechat_android.designsystem.components.ErrorScreen
import com.example.awarechat_android.designsystem.components.LoadingScreen
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme

class MainActivity : ComponentActivity() {
    private var localNetworkAccessState by mutableStateOf(LocalNetworkAccessState.CHECKING)

    private val requestLocalNetworkAccess = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        localNetworkAccessState = if (isGranted) {
            LocalNetworkAccessState.GRANTED
        } else {
            LocalNetworkAccessState.DENIED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        localNetworkAccessState = resolveLocalNetworkAccessState()
        val dependencies = (application as AwareChatApplication).dependencies
        setContent {
            AwareChatAndroidTheme {
                val surfaceModifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                when (localNetworkAccessState) {
                    LocalNetworkAccessState.CHECKING -> LoadingScreen(surfaceModifier)
                    LocalNetworkAccessState.GRANTED -> AwareChatApp(dependencies)
                    LocalNetworkAccessState.DENIED -> ErrorScreen(
                        message = stringResource(R.string.local_network_permission_required),
                        onRetry = ::requestLocalNetworkPermission,
                        onCancel = null,
                        modifier = surfaceModifier,
                    )
                }
            }
        }

        if (localNetworkAccessState == LocalNetworkAccessState.CHECKING) {
            requestLocalNetworkPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        if (
            localNetworkAccessState == LocalNetworkAccessState.DENIED &&
            hasLocalNetworkPermission()
        ) {
            localNetworkAccessState = LocalNetworkAccessState.GRANTED
        }
    }

    private fun requestLocalNetworkPermission() {
        if (!requiresLocalNetworkPermission()) {
            localNetworkAccessState = LocalNetworkAccessState.GRANTED
            return
        }

        localNetworkAccessState = LocalNetworkAccessState.CHECKING
        requestLocalNetworkAccess.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }

    private fun resolveLocalNetworkAccessState(): LocalNetworkAccessState =
        if (hasLocalNetworkPermission()) {
            LocalNetworkAccessState.GRANTED
        } else {
            LocalNetworkAccessState.CHECKING
        }

    private fun hasLocalNetworkPermission(): Boolean =
        !requiresLocalNetworkPermission() ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_LOCAL_NETWORK,
            ) == PackageManager.PERMISSION_GRANTED

    private fun requiresLocalNetworkPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN

    private enum class LocalNetworkAccessState {
        CHECKING,
        GRANTED,
        DENIED,
    }
}
