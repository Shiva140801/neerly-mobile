package com.neerly.mobile.feature.profile;

import com.neerly.mobile.data.api.NeerlyApi;
import com.neerly.mobile.data.auth.AuthRepository;
import com.neerly.mobile.data.repo.CustomerRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  private final Provider<AuthRepository> authProvider;

  private final Provider<NeerlyApi> apiProvider;

  public ProfileViewModel_Factory(Provider<CustomerRepository> repoProvider,
      Provider<AuthRepository> authProvider, Provider<NeerlyApi> apiProvider) {
    this.repoProvider = repoProvider;
    this.authProvider = authProvider;
    this.apiProvider = apiProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(repoProvider.get(), authProvider.get(), apiProvider.get());
  }

  public static ProfileViewModel_Factory create(Provider<CustomerRepository> repoProvider,
      Provider<AuthRepository> authProvider, Provider<NeerlyApi> apiProvider) {
    return new ProfileViewModel_Factory(repoProvider, authProvider, apiProvider);
  }

  public static ProfileViewModel newInstance(CustomerRepository repo, AuthRepository auth,
      NeerlyApi api) {
    return new ProfileViewModel(repo, auth, api);
  }
}
