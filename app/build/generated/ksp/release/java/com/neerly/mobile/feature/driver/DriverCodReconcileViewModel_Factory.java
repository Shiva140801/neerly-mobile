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
public final class DriverCodReconcileViewModel_Factory implements Factory<DriverCodReconcileViewModel> {
  private final Provider<DriverRepository> repoProvider;

  public DriverCodReconcileViewModel_Factory(Provider<DriverRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public DriverCodReconcileViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static DriverCodReconcileViewModel_Factory create(
      Provider<DriverRepository> repoProvider) {
    return new DriverCodReconcileViewModel_Factory(repoProvider);
  }

  public static DriverCodReconcileViewModel newInstance(DriverRepository repo) {
    return new DriverCodReconcileViewModel(repo);
  }
}
