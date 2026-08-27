package com.neerly.mobile.feature.vendor.orders;

import androidx.lifecycle.SavedStateHandle;
import com.neerly.mobile.data.repo.VendorRepository;
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
public final class VendorOrderDetailViewModel_Factory implements Factory<VendorOrderDetailViewModel> {
  private final Provider<VendorRepository> repoProvider;

  private final Provider<SavedStateHandle> savedStateProvider;

  public VendorOrderDetailViewModel_Factory(Provider<VendorRepository> repoProvider,
      Provider<SavedStateHandle> savedStateProvider) {
    this.repoProvider = repoProvider;
    this.savedStateProvider = savedStateProvider;
  }

  @Override
  public VendorOrderDetailViewModel get() {
    return newInstance(repoProvider.get(), savedStateProvider.get());
  }

  public static VendorOrderDetailViewModel_Factory create(Provider<VendorRepository> repoProvider,
      Provider<SavedStateHandle> savedStateProvider) {
    return new VendorOrderDetailViewModel_Factory(repoProvider, savedStateProvider);
  }

  public static VendorOrderDetailViewModel newInstance(VendorRepository repo,
      SavedStateHandle savedState) {
    return new VendorOrderDetailViewModel(repo, savedState);
  }
}
