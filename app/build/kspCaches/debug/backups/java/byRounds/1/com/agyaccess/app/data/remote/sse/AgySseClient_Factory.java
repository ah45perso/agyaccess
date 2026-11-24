package com.agyaccess.app.data.remote.sse;

import com.agyaccess.app.data.remote.network.SecureOkHttpProvider;
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
public final class AgySseClient_Factory implements Factory<AgySseClient> {
  private final Provider<SecureOkHttpProvider> okHttpProvider;

  private final Provider<AgySseEventParser> parserProvider;

  public AgySseClient_Factory(Provider<SecureOkHttpProvider> okHttpProvider,
      Provider<AgySseEventParser> parserProvider) {
    this.okHttpProvider = okHttpProvider;
    this.parserProvider = parserProvider;
  }

  @Override
  public AgySseClient get() {
    return newInstance(okHttpProvider.get(), parserProvider.get());
  }

  public static AgySseClient_Factory create(
      javax.inject.Provider<SecureOkHttpProvider> okHttpProvider,
      javax.inject.Provider<AgySseEventParser> parserProvider) {
    return new AgySseClient_Factory(Providers.asDaggerProvider(okHttpProvider), Providers.asDaggerProvider(parserProvider));
  }

  public static AgySseClient_Factory create(Provider<SecureOkHttpProvider> okHttpProvider,
      Provider<AgySseEventParser> parserProvider) {
    return new AgySseClient_Factory(okHttpProvider, parserProvider);
  }

  public static AgySseClient newInstance(SecureOkHttpProvider okHttpProvider,
      AgySseEventParser parser) {
    return new AgySseClient(okHttpProvider, parser);
  }
}
