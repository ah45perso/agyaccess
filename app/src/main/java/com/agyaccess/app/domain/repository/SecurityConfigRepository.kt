package com.agyaccess.app.domain.repository

import com.agyaccess.app.domain.model.NasConfig
import kotlinx.coroutines.flow.Flow

/**
 * Dépôt sécurisé pour la configuration NAS et les paramètres biométriques.
 */
interface SecurityConfigRepository {
    val nasConfigFlow: Flow<NasConfig>
    suspend fun getNasConfig(): NasConfig
    suspend fun saveNasConfig(config: NasConfig): Result<Unit>
    suspend fun clearSecrets()
}
