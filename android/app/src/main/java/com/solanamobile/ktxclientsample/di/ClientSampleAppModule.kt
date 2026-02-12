package com.solanamobile.ktxclientsample.di

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

val solanaUri = Uri.parse("https://kevredlabs.com")
val iconUri = Uri.parse("favicon.ico")
val identityName = "Seeker RPS"

@InstallIn(
    ViewModelComponent::class
)
@Module
class ClientSampleAppModule {

    @Provides
    fun providesSharedPrefs(@ApplicationContext ctx: Context): SharedPreferences {
        return ctx.getSharedPreferences("sample_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    fun providesMobileWalletAdapter(): MobileWalletAdapter {
        return MobileWalletAdapter(connectionIdentity = ConnectionIdentity(
            identityUri = solanaUri,
            iconUri = iconUri,
            identityName = identityName
        ))
    }

}