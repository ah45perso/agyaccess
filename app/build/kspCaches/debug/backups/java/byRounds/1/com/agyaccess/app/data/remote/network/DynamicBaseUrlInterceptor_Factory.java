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
public final class DynamicBaseUrlInterceptor_Factory implements Factory<DynamicBaseUrlInterceptor> {
  private final Provider<SecureConfigStorage> configStorageProvider;

  public DynamicBaseUrlInterceptor_Factory(Provider<SecureConfigStorage> configStorageProvider) {
    this.configStorageProvider = configStorageProvider;
  }

  @Override
  public DynamicBaseUrlInterceptor get() {
    return newInstance(configStorageProvider.get());
  }

  public static DynamicBaseUrlInterceptor_Factory create(
      javax.inject.Provider<SecureConfigStorage> configStorageProvider) {
    return new DynamicBaseUrlInterceptor_Factory(Providers.asDaggerProvider(configStorageProvider));
  }

  public static DynamicBaseUrlInterceptor_Factory create(
      Provider<SecureConfigStorage> configStorageProvider) {
    return new DynamicBaseUrlInterceptor_Factory(configStorageProvider);
  }

  public static DynamicBaseUrlInterceptor newInstance(SecureConfigStorage configStorage) {
    return new DynamicBaseUrlInterceptor(configStorage);
  }
}
