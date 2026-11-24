package com.agyaccess.app.data.repository

import com.agyaccess.app.data.security.SecureConfigStorage
import com.agyaccess.app.domain.model.NasConfig
import com.agyaccess.app.domain.repository.SecurityConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityConfigRepositoryImpl @Inject constructor(
    private val secureConfigStorage: SecureConfigStorage
) : SecurityConfigRepository {

    override val nasConfigFlow: Flow<NasConfig>
        get() = secureConfigStorage.configFlow

    override suspend fun getNasConfig(): NasConfig =
        secureConfigStorage.readConfig()

    override suspend fun saveNasConfig(config: NasConfig): Result<Unit> =
        secureConfigStorage.saveConfig(config)

    override suspend fun clearSecrets() {
        secureConfigStorage.clearAll()
    }
}
