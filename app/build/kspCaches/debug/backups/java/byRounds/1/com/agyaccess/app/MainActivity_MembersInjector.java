package com.agyaccess.app;

import com.agyaccess.app.data.security.BiometricAuthManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<BiometricAuthManager> biometricAuthManagerProvider;

  public MainActivity_MembersInjector(Provider<BiometricAuthManager> biometricAuthManagerProvider) {
    this.biometricAuthManagerProvider = biometricAuthManagerProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<BiometricAuthManager> biometricAuthManagerProvider) {
    return new MainActivity_MembersInjector(biometricAuthManagerProvider);
  }

  public static MembersInjector<MainActivity> create(
      javax.inject.Provider<BiometricAuthManager> biometricAuthManagerProvider) {
    return new MainActivity_MembersInjector(Providers.asDaggerProvider(biometricAuthManagerProvider));
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectBiometricAuthManager(instance, biometricAuthManagerProvider.get());
  }

  @InjectedFieldSignature("com.agyaccess.app.MainActivity.biometricAuthManager")
  public static void injectBiometricAuthManager(MainActivity instance,
      BiometricAuthManager biometricAuthManager) {
    instance.biometricAuthManager = biometricAuthManager;
  }
}
