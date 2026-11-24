package com.agyaccess.app.data.repository;

import com.agyaccess.app.data.remote.api.AgyApiService;
import com.agyaccess.app.data.session.ConcurrentSessionCoordinator;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AgyRepositoryImpl_Factory implements Factory<AgyRepositoryImpl> {
  private final Provider<AgyApiService> apiServiceProvider;

  private final Provider<ConcurrentSessionCoordinator> coordinatorProvider;

  public AgyRepositoryImpl_Factory(Provider<AgyApiService> apiServiceProvider,
      Provider<ConcurrentSessionCoordinator> coordinatorProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.coordinatorProvider = coordinatorProvider;
  }

  @Override
  public AgyRepositoryImpl get() {
    return newInstance(apiServiceProvider.get(), coordinatorProvider.get());
  }

  public static AgyRepositoryImpl_Factory create(
      javax.inject.Provider<AgyApiService> apiServiceProvider,
      javax.inject.Provider<ConcurrentSessionCoordinator> coordinatorProvider) {
    return new AgyRepositoryImpl_Factory(Providers.asDaggerProvider(apiServiceProvider), Providers.asDaggerProvider(coordinatorProvider));
  }

  public static AgyRepositoryImpl_Factory create(Provider<AgyApiService> apiServiceProvider,
      Provider<ConcurrentSessionCoordinator> coordinatorProvider) {
    return new AgyRepositoryImpl_Factory(apiServiceProvider, coordinatorProvider);
  }

  public static AgyRepositoryImpl newInstance(AgyApiService apiService,
      ConcurrentSessionCoordinator coordinator) {
    return new AgyRepositoryImpl(apiService, coordinator);
  }
}
