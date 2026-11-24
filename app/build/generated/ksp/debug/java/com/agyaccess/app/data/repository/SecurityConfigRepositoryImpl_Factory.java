package com.agyaccess.app.data.repository;

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
public final class SecurityConfigRepositoryImpl_Factory implements Factory<SecurityConfigRepositoryImpl> {
  private final Provider<SecureConfigStorage> secureConfigStorageProvider;

  public SecurityConfigRepositoryImpl_Factory(
      Provider<SecureConfigStorage> secureConfigStorageProvider) {
    this.secureConfigStorageProvider = secureConfigStorageProvider;
  }

  @Override
  public SecurityConfigRepositoryImpl get() {
    return newInstance(secureConfigStorageProvider.get());
  }

  public static SecurityConfigRepositoryImpl_Factory create(
      javax.inject.Provider<SecureConfigStorage> secureConfigStorageProvider) {
    return new SecurityConfigRepositoryImpl_Factory(Providers.asDaggerProvider(secureConfigStorageProvider));
  }

  public static SecurityConfigRepositoryImpl_Factory create(
      Provider<SecureConfigStorage> secureConfigStorageProvider) {
    return new SecurityConfigRepositoryImpl_Factory(secureConfigStorageProvider);
  }

  public static SecurityConfigRepositoryImpl newInstance(SecureConfigStorage secureConfigStorage) {
    return new SecurityConfigRepositoryImpl(secureConfigStorage);
  }
}
