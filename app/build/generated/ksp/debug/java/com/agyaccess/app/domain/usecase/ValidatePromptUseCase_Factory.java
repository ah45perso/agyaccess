package com.agyaccess.app.domain.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ValidatePromptUseCase_Factory implements Factory<ValidatePromptUseCase> {
  @Override
  public ValidatePromptUseCase get() {
    return newInstance();
  }

  public static ValidatePromptUseCase_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ValidatePromptUseCase newInstance() {
    return new ValidatePromptUseCase();
  }

  private static final class InstanceHolder {
    static final ValidatePromptUseCase_Factory INSTANCE = new ValidatePromptUseCase_Factory();
  }
}
