package com.agyaccess.app.presentation.chat;

import com.agyaccess.app.domain.repository.AgyRepository;
import com.agyaccess.app.domain.repository.SecurityConfigRepository;
import com.agyaccess.app.domain.usecase.CancelSessionUseCase;
import com.agyaccess.app.domain.usecase.CreateSessionUseCase;
import com.agyaccess.app.domain.usecase.ObserveSessionsRuntimeUseCase;
import com.agyaccess.app.domain.usecase.RefreshSessionsUseCase;
import com.agyaccess.app.domain.usecase.SendPromptUseCase;
import com.agyaccess.app.domain.usecase.ValidatePromptUseCase;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<AgyRepository> agyRepositoryProvider;

  private final Provider<SecurityConfigRepository> securityConfigRepositoryProvider;

  private final Provider<ObserveSessionsRuntimeUseCase> observeSessionsRuntimeProvider;

  private final Provider<RefreshSessionsUseCase> refreshSessionsUseCaseProvider;

  private final Provider<CreateSessionUseCase> createSessionUseCaseProvider;

  private final Provider<SendPromptUseCase> sendPromptUseCaseProvider;

  private final Provider<CancelSessionUseCase> cancelSessionUseCaseProvider;

  private final Provider<ValidatePromptUseCase> validatePromptUseCaseProvider;

  public ChatViewModel_Factory(Provider<AgyRepository> agyRepositoryProvider,
      Provider<SecurityConfigRepository> securityConfigRepositoryProvider,
      Provider<ObserveSessionsRuntimeUseCase> observeSessionsRuntimeProvider,
      Provider<RefreshSessionsUseCase> refreshSessionsUseCaseProvider,
      Provider<CreateSessionUseCase> createSessionUseCaseProvider,
      Provider<SendPromptUseCase> sendPromptUseCaseProvider,
      Provider<CancelSessionUseCase> cancelSessionUseCaseProvider,
      Provider<ValidatePromptUseCase> validatePromptUseCaseProvider) {
    this.agyRepositoryProvider = agyRepositoryProvider;
    this.securityConfigRepositoryProvider = securityConfigRepositoryProvider;
    this.observeSessionsRuntimeProvider = observeSessionsRuntimeProvider;
    this.refreshSessionsUseCaseProvider = refreshSessionsUseCaseProvider;
    this.createSessionUseCaseProvider = createSessionUseCaseProvider;
    this.sendPromptUseCaseProvider = sendPromptUseCaseProvider;
    this.cancelSessionUseCaseProvider = cancelSessionUseCaseProvider;
    this.validatePromptUseCaseProvider = validatePromptUseCaseProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(agyRepositoryProvider.get(), securityConfigRepositoryProvider.get(), observeSessionsRuntimeProvider.get(), refreshSessionsUseCaseProvider.get(), createSessionUseCaseProvider.get(), sendPromptUseCaseProvider.get(), cancelSessionUseCaseProvider.get(), validatePromptUseCaseProvider.get());
  }

  public static ChatViewModel_Factory create(
      javax.inject.Provider<AgyRepository> agyRepositoryProvider,
      javax.inject.Provider<SecurityConfigRepository> securityConfigRepositoryProvider,
      javax.inject.Provider<ObserveSessionsRuntimeUseCase> observeSessionsRuntimeProvider,
      javax.inject.Provider<RefreshSessionsUseCase> refreshSessionsUseCaseProvider,
      javax.inject.Provider<CreateSessionUseCase> createSessionUseCaseProvider,
      javax.inject.Provider<SendPromptUseCase> sendPromptUseCaseProvider,
      javax.inject.Provider<CancelSessionUseCase> cancelSessionUseCaseProvider,
      javax.inject.Provider<ValidatePromptUseCase> validatePromptUseCaseProvider) {
    return new ChatViewModel_Factory(Providers.asDaggerProvider(agyRepositoryProvider), Providers.asDaggerProvider(securityConfigRepositoryProvider), Providers.asDaggerProvider(observeSessionsRuntimeProvider), Providers.asDaggerProvider(refreshSessionsUseCaseProvider), Providers.asDaggerProvider(createSessionUseCaseProvider), Providers.asDaggerProvider(sendPromptUseCaseProvider), Providers.asDaggerProvider(cancelSessionUseCaseProvider), Providers.asDaggerProvider(validatePromptUseCaseProvider));
  }

  public static ChatViewModel_Factory create(Provider<AgyRepository> agyRepositoryProvider,
      Provider<SecurityConfigRepository> securityConfigRepositoryProvider,
      Provider<ObserveSessionsRuntimeUseCase> observeSessionsRuntimeProvider,
      Provider<RefreshSessionsUseCase> refreshSessionsUseCaseProvider,
      Provider<CreateSessionUseCase> createSessionUseCaseProvider,
      Provider<SendPromptUseCase> sendPromptUseCaseProvider,
      Provider<CancelSessionUseCase> cancelSessionUseCaseProvider,
      Provider<ValidatePromptUseCase> validatePromptUseCaseProvider) {
    return new ChatViewModel_Factory(agyRepositoryProvider, securityConfigRepositoryProvider, observeSessionsRuntimeProvider, refreshSessionsUseCaseProvider, createSessionUseCaseProvider, sendPromptUseCaseProvider, cancelSessionUseCaseProvider, validatePromptUseCaseProvider);
  }

  public static ChatViewModel newInstance(AgyRepository agyRepository,
      SecurityConfigRepository securityConfigRepository,
      ObserveSessionsRuntimeUseCase observeSessionsRuntime,
      RefreshSessionsUseCase refreshSessionsUseCase, CreateSessionUseCase createSessionUseCase,
      SendPromptUseCase sendPromptUseCase, CancelSessionUseCase cancelSessionUseCase,
      ValidatePromptUseCase validatePromptUseCase) {
    return new ChatViewModel(agyRepository, securityConfigRepository, observeSessionsRuntime, refreshSessionsUseCase, createSessionUseCase, sendPromptUseCase, cancelSessionUseCase, validatePromptUseCase);
  }
}
