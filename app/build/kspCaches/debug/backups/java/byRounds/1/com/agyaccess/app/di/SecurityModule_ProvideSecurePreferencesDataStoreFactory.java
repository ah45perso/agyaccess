package com.agyaccess.app.di;

import android.content.Context;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SecurityModule_ProvideSecurePreferencesDataStoreFactory implements Factory<DataStore<Preferences>> {
  private final Provider<Context> contextProvider;

  public SecurityModule_ProvideSecurePreferencesDataStoreFactory(
      Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DataStore<Preferences> get() {
    return provideSecurePreferencesDataStore(contextProvider.get());
  }

  public static SecurityModule_ProvideSecurePreferencesDataStoreFactory create(
      javax.inject.Provider<Context> contextProvider) {
    return new SecurityModule_ProvideSecurePreferencesDataStoreFactory(Providers.asDaggerProvider(contextProvider));
  }

  public static SecurityModule_ProvideSecurePreferencesDataStoreFactory create(
      Provider<Context> contextProvider) {
    return new SecurityModule_ProvideSecurePreferencesDataStoreFactory(contextProvider);
  }

  public static DataStore<Preferences> provideSecurePreferencesDataStore(Context context) {
    return Preconditions.checkNotNullFromProvides(SecurityModule.INSTANCE.provideSecurePreferencesDataStore(context));
  }
}
