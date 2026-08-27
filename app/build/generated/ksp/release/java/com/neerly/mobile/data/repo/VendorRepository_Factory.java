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
public final class VendorRepository_Factory implements Factory<VendorRepository> {
  private final Provider<NeerlyApi> apiProvider;

  public VendorRepository_Factory(Provider<NeerlyApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public VendorRepository get() {
    return newInstance(apiProvider.get());
  }

  public static VendorRepository_Factory create(Provider<NeerlyApi> apiProvider) {
    return new VendorRepository_Factory(apiProvider);
  }

  public static VendorRepository newInstance(NeerlyApi api) {
    return new VendorRepository(api);
  }
}
