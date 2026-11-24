package com.agyaccess.app.data.remote.network;

import com.agyaccess.app.data.security.SecureConfigStorage;
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
public final class AuthHeaderInterceptor_Factory implements Factory<AuthHeaderInterceptor> {
  private final Provider<SecureConfigStorage> configStorageProvider;

  public AuthHeaderInterceptor_Factory(Provider<SecureConfigStorage> configStorageProvider) {
    this.configStorageProvider = configStorageProvider;
  }

  @Override
  public AuthHeaderInterceptor get() {
    return newInstance(configStorageProvider.get());
  }

  public static AuthHeaderInterceptor_Factory create(
      javax.inject.Provider<SecureConfigStorage> configStorageProvider) {
    return new AuthHeaderInterceptor_Factory(Providers.asDaggerProvider(configStorageProvider));
  }

  public static AuthHeaderInterceptor_Factory create(
      Provider<SecureConfigStorage> configStorageProvider) {
    return new AuthHeaderInterceptor_Factory(configStorageProvider);
  }

  public static AuthHeaderInterceptor newInstance(SecureConfigStorage configStorage) {
    return new AuthHeaderInterceptor(configStorage);
  }
}
