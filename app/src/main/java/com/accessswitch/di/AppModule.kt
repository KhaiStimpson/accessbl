package com.accessswitch.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module providing app-level dependencies.
 *
 * Most dependencies (ScanningEngine, SwitchInputHub, SettingsRepository)
 * are @Singleton @Inject constructor classes and don't need explicit
 * @Provides methods — Hilt handles them automatically.
 *
 * This module exists for dependencies that need manual construction
 * (e.g., system services, third-party objects).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Future: provide AudioManager, TelecomManager, BluetoothManager, etc.
}
