package com.neerly.mobile.data.api;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
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
    "deprecation"
})
public final class NetworkModule_ProvideNeerlyApiFactory implements Factory<NeerlyApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideNeerlyApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public NeerlyApi get() {
    return provideNeerlyApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideNeerlyApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideNeerlyApiFactory(retrofitProvider);
  }

  public static NeerlyApi provideNeerlyApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideNeerlyApi(retrofit));
  }
}
