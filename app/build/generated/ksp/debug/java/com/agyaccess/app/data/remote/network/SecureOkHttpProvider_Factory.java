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
public final class SecureOkHttpProvider_Factory implements Factory<SecureOkHttpProvider> {
  private final Provider<SecureConfigStorage> configStorageProvider;

  private final Provider<DynamicBaseUrlInterceptor> dynamicBaseUrlInterceptorProvider;

  private final Provider<AuthHeaderInterceptor> authHeaderInterceptorProvider;

  public SecureOkHttpProvider_Factory(Provider<SecureConfigStorage> configStorageProvider,
      Provider<DynamicBaseUrlInterceptor> dynamicBaseUrlInterceptorProvider,
      Provider<AuthHeaderInterceptor> authHeaderInterceptorProvider) {
    this.configStorageProvider = configStorageProvider;
    this.dynamicBaseUrlInterceptorProvider = dynamicBaseUrlInterceptorProvider;
    this.authHeaderInterceptorProvider = authHeaderInterceptorProvider;
  }

  @Override
  public SecureOkHttpProvider get() {
    return newInstance(configStorageProvider.get(), dynamicBaseUrlInterceptorProvider.get(), authHeaderInterceptorProvider.get());
  }

  public static SecureOkHttpProvider_Factory create(
      javax.inject.Provider<SecureConfigStorage> configStorageProvider,
      javax.inject.Provider<DynamicBaseUrlInterceptor> dynamicBaseUrlInterceptorProvider,
      javax.inject.Provider<AuthHeaderInterceptor> authHeaderInterceptorProvider) {
    return new SecureOkHttpProvider_Factory(Providers.asDaggerProvider(configStorageProvider), Providers.asDaggerProvider(dynamicBaseUrlInterceptorProvider), Providers.asDaggerProvider(authHeaderInterceptorProvider));
  }

  public static SecureOkHttpProvider_Factory create(
      Provider<SecureConfigStorage> configStorageProvider,
      Provider<DynamicBaseUrlInterceptor> dynamicBaseUrlInterceptorProvider,
      Provider<AuthHeaderInterceptor> authHeaderInterceptorProvider) {
    return new SecureOkHttpProvider_Factory(configStorageProvider, dynamicBaseUrlInterceptorProvider, authHeaderInterceptorProvider);
  }

  public static SecureOkHttpProvider newInstance(SecureConfigStorage configStorage,
      DynamicBaseUrlInterceptor dynamicBaseUrlInterceptor,
      AuthHeaderInterceptor authHeaderInterceptor) {
    return new SecureOkHttpProvider(configStorage, dynamicBaseUrlInterceptor, authHeaderInterceptor);
  }
}
