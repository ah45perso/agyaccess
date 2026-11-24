package com.agyaccess.app.di;

import com.agyaccess.app.data.remote.network.SecureOkHttpProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideRetrofitFactory implements Factory<Retrofit> {
  private final Provider<SecureOkHttpProvider> okHttpProvider;

  private final Provider<Json> jsonProvider;

  public NetworkModule_ProvideRetrofitFactory(Provider<SecureOkHttpProvider> okHttpProvider,
      Provider<Json> jsonProvider) {
    this.okHttpProvider = okHttpProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public Retrofit get() {
    return provideRetrofit(okHttpProvider.get(), jsonProvider.get());
  }

  public static NetworkModule_ProvideRetrofitFactory create(
      javax.inject.Provider<SecureOkHttpProvider> okHttpProvider,
      javax.inject.Provider<Json> jsonProvider) {
    return new NetworkModule_ProvideRetrofitFactory(Providers.asDaggerProvider(okHttpProvider), Providers.asDaggerProvider(jsonProvider));
  }

  public static NetworkModule_ProvideRetrofitFactory create(
      Provider<SecureOkHttpProvider> okHttpProvider, Provider<Json> jsonProvider) {
    return new NetworkModule_ProvideRetrofitFactory(okHttpProvider, jsonProvider);
  }

  public static Retrofit provideRetrofit(SecureOkHttpProvider okHttpProvider, Json json) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideRetrofit(okHttpProvider, json));
  }
}
