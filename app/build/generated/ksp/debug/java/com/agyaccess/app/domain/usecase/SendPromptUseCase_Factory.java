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
public final class SendPromptUseCase_Factory implements Factory<SendPromptUseCase> {
  private final Provider<AgyRepository> repositoryProvider;

  public SendPromptUseCase_Factory(Provider<AgyRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SendPromptUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static SendPromptUseCase_Factory create(
      javax.inject.Provider<AgyRepository> repositoryProvider) {
    return new SendPromptUseCase_Factory(Providers.asDaggerProvider(repositoryProvider));
  }

  public static SendPromptUseCase_Factory create(Provider<AgyRepository> repositoryProvider) {
    return new SendPromptUseCase_Factory(repositoryProvider);
  }

  public static SendPromptUseCase newInstance(AgyRepository repository) {
    return new SendPromptUseCase(repository);
  }
}
