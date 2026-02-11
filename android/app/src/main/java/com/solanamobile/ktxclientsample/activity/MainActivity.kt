package com.solanamobile.ktxclientsample.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.solanamobile.ktxclientsample.ui.HelloWorldScreen
import com.solanamobile.ktxclientsample.ui.theme.KtxClientSampleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KtxClientSampleTheme {
                HelloWorldScreen()
            }
        }
    }
}