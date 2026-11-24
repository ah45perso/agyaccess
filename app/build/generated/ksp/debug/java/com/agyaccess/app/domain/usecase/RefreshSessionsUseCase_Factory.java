package com.agyaccess.app.domain.usecase;

import com.agyaccess.app.domain.repository.AgyRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class RefreshSessionsUseCase_Factory implements Factory<RefreshSessionsUseCase> {
  private final Provider<AgyRepository> repositoryProvider;

  public RefreshSessionsUseCase_Factory(Provider<AgyRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public RefreshSessionsUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static RefreshSessionsUseCase_Factory create(
      javax.inject.Provider<AgyRepository> repositoryProvider) {
    return new RefreshSessionsUseCase_Factory(Providers.asDaggerProvider(repositoryProvider));
  }

  public static RefreshSessionsUseCase_Factory create(Provider<AgyRepository> repositoryProvider) {
    return new RefreshSessionsUseCase_Factory(repositoryProvider);
  }

  public static RefreshSessionsUseCase newInstance(AgyRepository repository) {
    return new RefreshSessionsUseCase(repository);
  }
}
