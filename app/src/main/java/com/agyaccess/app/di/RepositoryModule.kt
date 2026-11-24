package com.agyaccess.app.di

import com.agyaccess.app.data.repository.AgyRepositoryImpl
import com.agyaccess.app.data.repository.SecurityConfigRepositoryImpl
import com.agyaccess.app.domain.repository.AgyRepository
import com.agyaccess.app.domain.repository.SecurityConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAgyRepository(impl: AgyRepositoryImpl): AgyRepository

    @Binds
    @Singleton
    abstract fun bindSecurityConfigRepository(
        impl: SecurityConfigRepositoryImpl
    ): SecurityConfigRepository
}
