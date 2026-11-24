package com.agyaccess.app.data.session;

import com.agyaccess.app.data.remote.sse.AgySseClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.agyaccess.app.di.ApplicationScope")
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
public final class ConcurrentSessionCoordinator_Factory implements Factory<ConcurrentSessionCoordinator> {
  private final Provider<AgySseClient> sseClientProvider;

  private final Provider<CoroutineScope> appScopeProvider;

  public ConcurrentSessionCoordinator_Factory(Provider<AgySseClient> sseClientProvider,
      Provider<CoroutineScope> appScopeProvider) {
    this.sseClientProvider = sseClientProvider;
    this.appScopeProvider = appScopeProvider;
  }

  @Override
  public ConcurrentSessionCoordinator get() {
    return newInstance(sseClientProvider.get(), appScopeProvider.get());
  }

  public static ConcurrentSessionCoordinator_Factory create(
      javax.inject.Provider<AgySseClient> sseClientProvider,
      javax.inject.Provider<CoroutineScope> appScopeProvider) {
    return new ConcurrentSessionCoordinator_Factory(Providers.asDaggerProvider(sseClientProvider), Providers.asDaggerProvider(appScopeProvider));
  }

  public static ConcurrentSessionCoordinator_Factory create(
      Provider<AgySseClient> sseClientProvider, Provider<CoroutineScope> appScopeProvider) {
    return new ConcurrentSessionCoordinator_Factory(sseClientProvider, appScopeProvider);
  }

  public static ConcurrentSessionCoordinator newInstance(AgySseClient sseClient,
      CoroutineScope appScope) {
    return new ConcurrentSessionCoordinator(sseClient, appScope);
  }
}
