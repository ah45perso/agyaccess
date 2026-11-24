package com.agyaccess.app.data.security;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.serialization.json.Json;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.agyaccess.app.di.ApplicationScope")
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
public final class SecureConfigStorage_Factory implements Factory<SecureConfigStorage> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  private final Provider<KeystoreCryptoManager> cryptoManagerProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<CoroutineScope> appScopeProvider;

  public SecureConfigStorage_Factory(Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<KeystoreCryptoManager> cryptoManagerProvider, Provider<Json> jsonProvider,
      Provider<CoroutineScope> appScopeProvider) {
    this.dataStoreProvider = dataStoreProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.jsonProvider = jsonProvider;
    this.appScopeProvider = appScopeProvider;
  }

  @Override
  public SecureConfigStorage get() {
    return newInstance(dataStoreProvider.get(), cryptoManagerProvider.get(), jsonProvider.get(), appScopeProvider.get());
  }

  public static SecureConfigStorage_Factory create(
      javax.inject.Provider<DataStore<Preferences>> dataStoreProvider,
      javax.inject.Provider<KeystoreCryptoManager> cryptoManagerProvider,
      javax.inject.Provider<Json> jsonProvider,
      javax.inject.Provider<CoroutineScope> appScopeProvider) {
    return new SecureConfigStorage_Factory(Providers.asDaggerProvider(dataStoreProvider), Providers.asDaggerProvider(cryptoManagerProvider), Providers.asDaggerProvider(jsonProvider), Providers.asDaggerProvider(appScopeProvider));
  }

  public static SecureConfigStorage_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<KeystoreCryptoManager> cryptoManagerProvider, Provider<Json> jsonProvider,
      Provider<CoroutineScope> appScopeProvider) {
    return new SecureConfigStorage_Factory(dataStoreProvider, cryptoManagerProvider, jsonProvider, appScopeProvider);
  }

  public static SecureConfigStorage newInstance(DataStore<Preferences> dataStore,
      KeystoreCryptoManager cryptoManager, Json json, CoroutineScope appScope) {
    return new SecureConfigStorage(dataStore, cryptoManager, json, appScope);
  }
}
