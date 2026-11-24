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
public final class CancelSessionUseCase_Factory implements Factory<CancelSessionUseCase> {
  private final Provider<AgyRepository> repositoryProvider;

  public CancelSessionUseCase_Factory(Provider<AgyRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public CancelSessionUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static CancelSessionUseCase_Factory create(
      javax.inject.Provider<AgyRepository> repositoryProvider) {
    return new CancelSessionUseCase_Factory(Providers.asDaggerProvider(repositoryProvider));
  }

  public static CancelSessionUseCase_Factory create(Provider<AgyRepository> repositoryProvider) {
    return new CancelSessionUseCase_Factory(repositoryProvider);
  }

  public static CancelSessionUseCase newInstance(AgyRepository repository) {
    return new CancelSessionUseCase(repository);
  }
}
