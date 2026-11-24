package com.agyaccess.app;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.agyaccess.app.data.remote.api.AgyApiService;
import com.agyaccess.app.data.remote.network.AuthHeaderInterceptor;
import com.agyaccess.app.data.remote.network.DynamicBaseUrlInterceptor;
import com.agyaccess.app.data.remote.network.SecureOkHttpProvider;
import com.agyaccess.app.data.remote.sse.AgySseClient;
import com.agyaccess.app.data.remote.sse.AgySseEventParser;
import com.agyaccess.app.data.repository.AgyRepositoryImpl;
import com.agyaccess.app.data.repository.SecurityConfigRepositoryImpl;
import com.agyaccess.app.data.security.BiometricAuthManager;
import com.agyaccess.app.data.security.KeystoreCryptoManager;
import com.agyaccess.app.data.security.SecureConfigStorage;
import com.agyaccess.app.data.session.ConcurrentSessionCoordinator;
import com.agyaccess.app.di.AppModule_ProvideApplicationScopeFactory;
import com.agyaccess.app.di.AppModule_ProvideJsonFactory;
import com.agyaccess.app.di.NetworkModule_ProvideAgyApiServiceFactory;
import com.agyaccess.app.di.NetworkModule_ProvideRetrofitFactory;
import com.agyaccess.app.di.SecurityModule_ProvideSecurePreferencesDataStoreFactory;
import com.agyaccess.app.domain.usecase.CancelSessionUseCase;
import com.agyaccess.app.domain.usecase.CreateSessionUseCase;
import com.agyaccess.app.domain.usecase.ObserveSessionsRuntimeUseCase;
import com.agyaccess.app.domain.usecase.RefreshSessionsUseCase;
import com.agyaccess.app.domain.usecase.SendPromptUseCase;
import com.agyaccess.app.domain.usecase.ValidatePromptUseCase;
import com.agyaccess.app.presentation.chat.ChatViewModel;
import com.agyaccess.app.presentation.chat.ChatViewModel_HiltModules;
import com.agyaccess.app.presentation.chat.ChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.agyaccess.app.presentation.chat.ChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.serialization.json.Json;
import retrofit2.Retrofit;

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
public final class DaggerAgyApplication_HiltComponents_SingletonC {
  private DaggerAgyApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public AgyApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements AgyApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public AgyApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements AgyApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public AgyApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements AgyApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public AgyApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements AgyApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public AgyApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements AgyApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public AgyApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements AgyApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public AgyApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements AgyApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public AgyApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends AgyApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends AgyApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends AgyApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends AgyApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
      injectMainActivity2(arg0);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(Collections.<String, Boolean>singletonMap(ChatViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ChatViewModel_HiltModules.KeyModule.provide()));
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectBiometricAuthManager(instance, singletonCImpl.biometricAuthManagerProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends AgyApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<ChatViewModel> chatViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private ObserveSessionsRuntimeUseCase observeSessionsRuntimeUseCase() {
      return new ObserveSessionsRuntimeUseCase(singletonCImpl.agyRepositoryImplProvider.get());
    }

    private RefreshSessionsUseCase refreshSessionsUseCase() {
      return new RefreshSessionsUseCase(singletonCImpl.agyRepositoryImplProvider.get());
    }

    private CreateSessionUseCase createSessionUseCase() {
      return new CreateSessionUseCase(singletonCImpl.agyRepositoryImplProvider.get());
    }

    private SendPromptUseCase sendPromptUseCase() {
      return new SendPromptUseCase(singletonCImpl.agyRepositoryImplProvider.get());
    }

    private CancelSessionUseCase cancelSessionUseCase() {
      return new CancelSessionUseCase(singletonCImpl.agyRepositoryImplProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.chatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(Collections.<String, javax.inject.Provider<ViewModel>>singletonMap(ChatViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) chatViewModelProvider)));
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.agyaccess.app.presentation.chat.ChatViewModel 
          return (T) new ChatViewModel(singletonCImpl.agyRepositoryImplProvider.get(), singletonCImpl.securityConfigRepositoryImplProvider.get(), viewModelCImpl.observeSessionsRuntimeUseCase(), viewModelCImpl.refreshSessionsUseCase(), viewModelCImpl.createSessionUseCase(), viewModelCImpl.sendPromptUseCase(), viewModelCImpl.cancelSessionUseCase(), new ValidatePromptUseCase());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends AgyApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends AgyApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends AgyApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<BiometricAuthManager> biometricAuthManagerProvider;

    private Provider<DataStore<Preferences>> provideSecurePreferencesDataStoreProvider;

    private Provider<KeystoreCryptoManager> keystoreCryptoManagerProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<CoroutineScope> provideApplicationScopeProvider;

    private Provider<SecureConfigStorage> secureConfigStorageProvider;

    private Provider<DynamicBaseUrlInterceptor> dynamicBaseUrlInterceptorProvider;

    private Provider<AuthHeaderInterceptor> authHeaderInterceptorProvider;

    private Provider<SecureOkHttpProvider> secureOkHttpProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<AgyApiService> provideAgyApiServiceProvider;

    private Provider<AgySseEventParser> agySseEventParserProvider;

    private Provider<AgySseClient> agySseClientProvider;

    private Provider<ConcurrentSessionCoordinator> concurrentSessionCoordinatorProvider;

    private Provider<AgyRepositoryImpl> agyRepositoryImplProvider;

    private Provider<SecurityConfigRepositoryImpl> securityConfigRepositoryImplProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.biometricAuthManagerProvider = DoubleCheck.provider(new SwitchingProvider<BiometricAuthManager>(singletonCImpl, 0));
      this.provideSecurePreferencesDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 6));
      this.keystoreCryptoManagerProvider = DoubleCheck.provider(new SwitchingProvider<KeystoreCryptoManager>(singletonCImpl, 7));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 8));
      this.provideApplicationScopeProvider = DoubleCheck.provider(new SwitchingProvider<CoroutineScope>(singletonCImpl, 9));
      this.secureConfigStorageProvider = DoubleCheck.provider(new SwitchingProvider<SecureConfigStorage>(singletonCImpl, 5));
      this.dynamicBaseUrlInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<DynamicBaseUrlInterceptor>(singletonCImpl, 10));
      this.authHeaderInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<AuthHeaderInterceptor>(singletonCImpl, 11));
      this.secureOkHttpProvider = DoubleCheck.provider(new SwitchingProvider<SecureOkHttpProvider>(singletonCImpl, 4));
      this.provideRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 3));
      this.provideAgyApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<AgyApiService>(singletonCImpl, 2));
      this.agySseEventParserProvider = DoubleCheck.provider(new SwitchingProvider<AgySseEventParser>(singletonCImpl, 14));
      this.agySseClientProvider = DoubleCheck.provider(new SwitchingProvider<AgySseClient>(singletonCImpl, 13));
      this.concurrentSessionCoordinatorProvider = DoubleCheck.provider(new SwitchingProvider<ConcurrentSessionCoordinator>(singletonCImpl, 12));
      this.agyRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AgyRepositoryImpl>(singletonCImpl, 1));
      this.securityConfigRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SecurityConfigRepositoryImpl>(singletonCImpl, 15));
    }

    @Override
    public void injectAgyApplication(AgyApplication arg0) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.agyaccess.app.data.security.BiometricAuthManager 
          return (T) new BiometricAuthManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.agyaccess.app.data.repository.AgyRepositoryImpl 
          return (T) new AgyRepositoryImpl(singletonCImpl.provideAgyApiServiceProvider.get(), singletonCImpl.concurrentSessionCoordinatorProvider.get());

          case 2: // com.agyaccess.app.data.remote.api.AgyApiService 
          return (T) NetworkModule_ProvideAgyApiServiceFactory.provideAgyApiService(singletonCImpl.provideRetrofitProvider.get());

          case 3: // retrofit2.Retrofit 
          return (T) NetworkModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.secureOkHttpProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 4: // com.agyaccess.app.data.remote.network.SecureOkHttpProvider 
          return (T) new SecureOkHttpProvider(singletonCImpl.secureConfigStorageProvider.get(), singletonCImpl.dynamicBaseUrlInterceptorProvider.get(), singletonCImpl.authHeaderInterceptorProvider.get());

          case 5: // com.agyaccess.app.data.security.SecureConfigStorage 
          return (T) new SecureConfigStorage(singletonCImpl.provideSecurePreferencesDataStoreProvider.get(), singletonCImpl.keystoreCryptoManagerProvider.get(), singletonCImpl.provideJsonProvider.get(), singletonCImpl.provideApplicationScopeProvider.get());

          case 6: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) SecurityModule_ProvideSecurePreferencesDataStoreFactory.provideSecurePreferencesDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // com.agyaccess.app.data.security.KeystoreCryptoManager 
          return (T) new KeystoreCryptoManager();

          case 8: // kotlinx.serialization.json.Json 
          return (T) AppModule_ProvideJsonFactory.provideJson();

          case 9: // @com.agyaccess.app.di.ApplicationScope kotlinx.coroutines.CoroutineScope 
          return (T) AppModule_ProvideApplicationScopeFactory.provideApplicationScope();

          case 10: // com.agyaccess.app.data.remote.network.DynamicBaseUrlInterceptor 
          return (T) new DynamicBaseUrlInterceptor(singletonCImpl.secureConfigStorageProvider.get());

          case 11: // com.agyaccess.app.data.remote.network.AuthHeaderInterceptor 
          return (T) new AuthHeaderInterceptor(singletonCImpl.secureConfigStorageProvider.get());

          case 12: // com.agyaccess.app.data.session.ConcurrentSessionCoordinator 
          return (T) new ConcurrentSessionCoordinator(singletonCImpl.agySseClientProvider.get(), singletonCImpl.provideApplicationScopeProvider.get());

          case 13: // com.agyaccess.app.data.remote.sse.AgySseClient 
          return (T) new AgySseClient(singletonCImpl.secureOkHttpProvider.get(), singletonCImpl.agySseEventParserProvider.get());

          case 14: // com.agyaccess.app.data.remote.sse.AgySseEventParser 
          return (T) new AgySseEventParser(singletonCImpl.provideJsonProvider.get());

          case 15: // com.agyaccess.app.data.repository.SecurityConfigRepositoryImpl 
          return (T) new SecurityConfigRepositoryImpl(singletonCImpl.secureConfigStorageProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
