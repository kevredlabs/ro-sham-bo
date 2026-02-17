package com.solanamobile.ktxclientsample.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solanamobile.ktxclientsample.ui.theme.PixelCyan
import com.solanamobile.ktxclientsample.ui.theme.PixelDarkGray
import com.solanamobile.ktxclientsample.ui.theme.PixelLightGray
import com.solanamobile.ktxclientsample.ui.theme.PixelWhite
import com.solanamobile.ktxclientsample.ui.theme.PixelYellow
import com.solanamobile.ktxclientsample.viewmodel.GameViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SampleScreen(
    intentSender: ActivityResultSender,
    viewModel: GameViewModel = hiltViewModel()
) {
    val viewState = viewModel.viewState.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.viewState.collectLatest { viewState ->
            if (viewState.error.isNotEmpty()) {
                snackbarHostState.showSnackbar(viewState.error, "DISMISS").let { result ->
                    if (result == SnackbarResult.ActionPerformed) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }
                }
            }
        }
        viewModel.loadConnection()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        backgroundColor = MaterialTheme.colors.background
    ) { padding ->
        PixelScreen(modifier = Modifier.padding(padding)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, PixelDarkGray, RectangleShape)
                            .padding(top = 12.dp, bottom = 12.dp),
                        text = "KTX CLIENT",
                        style = MaterialTheme.typography.h4,
                        color = PixelCyan,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        if (viewState.walletFound && viewState.userAddress.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VpnKey,
                                    contentDescription = "Address",
                                    tint = PixelYellow,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 8.dp)
                                )
                                val accountLabel =
                                    if (viewState.userLabel.isNotEmpty()) {
                                        "${viewState.userLabel} - ${viewState.userAddress}"
                                    } else {
                                        viewState.userAddress
                                    }
                                Text(
                                    text = accountLabel,
                                    style = MaterialTheme.typography.body2,
                                    color = PixelWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BAL: ",
                                    style = MaterialTheme.typography.body1,
                                    color = PixelLightGray,
                                    maxLines = 1
                                )
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = if (viewState.solBalance >= 0) viewState.solBalance.toString() else "-",
                                    style = MaterialTheme.typography.body1,
                                    color = PixelCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            PixelOutlinedButton(
                                text = "Disconnect",
                                onClick = { viewModel.disconnect(intentSender) },
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = PixelLightGray,
                                textColor = PixelLightGray,
                                buttonHeight = 40.dp
                            )
                        } else {
                            PixelButton(
                                text = "Sign In",
                                onClick = { viewModel.signIn(intentSender) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (viewState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(8.dp)
                            .height(48.dp)
                            .width(48.dp)
                            .align(Alignment.Center),
                        color = PixelCyan
                    )
                }
            }
        }
    }
}
