package com.neerly.mobile.data.repo;

import com.neerly.mobile.data.api.NeerlyApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TrustRepository_Factory implements Factory<TrustRepository> {
  private final Provider<NeerlyApi> apiProvider;

  public TrustRepository_Factory(Provider<NeerlyApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public TrustRepository get() {
    return newInstance(apiProvider.get());
  }

  public static TrustRepository_Factory create(Provider<NeerlyApi> apiProvider) {
    return new TrustRepository_Factory(apiProvider);
  }

  public static TrustRepository newInstance(NeerlyApi api) {
    return new TrustRepository(api);
  }
}
