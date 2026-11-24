package com.agyaccess.app.data.remote.sse;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;

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
public final class AgySseEventParser_Factory implements Factory<AgySseEventParser> {
  private final Provider<Json> jsonProvider;

  public AgySseEventParser_Factory(Provider<Json> jsonProvider) {
    this.jsonProvider = jsonProvider;
  }

  @Override
  public AgySseEventParser get() {
    return newInstance(jsonProvider.get());
  }

  public static AgySseEventParser_Factory create(javax.inject.Provider<Json> jsonProvider) {
    return new AgySseEventParser_Factory(Providers.asDaggerProvider(jsonProvider));
  }

  public static AgySseEventParser_Factory create(Provider<Json> jsonProvider) {
    return new AgySseEventParser_Factory(jsonProvider);
  }

  public static AgySseEventParser newInstance(Json json) {
    return new AgySseEventParser(json);
  }
}
