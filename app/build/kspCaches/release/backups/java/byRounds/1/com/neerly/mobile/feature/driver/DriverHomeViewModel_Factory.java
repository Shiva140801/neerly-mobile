package com.neerly.mobile.feature.driver;

import com.neerly.mobile.data.repo.DriverRepository;
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
public final class DriverHomeViewModel_Factory implements Factory<DriverHomeViewModel> {
  private final Provider<DriverRepository> repoProvider;

  public DriverHomeViewModel_Factory(Provider<DriverRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public DriverHomeViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static DriverHomeViewModel_Factory create(Provider<DriverRepository> repoProvider) {
    return new DriverHomeViewModel_Factory(repoProvider);
  }

  public static DriverHomeViewModel newInstance(DriverRepository repo) {
    return new DriverHomeViewModel(repo);
  }
}
