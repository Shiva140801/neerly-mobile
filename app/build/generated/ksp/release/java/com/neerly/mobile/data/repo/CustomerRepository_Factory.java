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
public final class CustomerRepository_Factory implements Factory<CustomerRepository> {
  private final Provider<NeerlyApi> apiProvider;

  public CustomerRepository_Factory(Provider<NeerlyApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public CustomerRepository get() {
    return newInstance(apiProvider.get());
  }

  public static CustomerRepository_Factory create(Provider<NeerlyApi> apiProvider) {
    return new CustomerRepository_Factory(apiProvider);
  }

  public static CustomerRepository newInstance(NeerlyApi api) {
    return new CustomerRepository(api);
  }
}
