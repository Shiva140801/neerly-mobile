package com.neerly.mobile.feature.complaint;

import androidx.lifecycle.SavedStateHandle;
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
public final class ComplaintThreadViewModel_Factory implements Factory<ComplaintThreadViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  private final Provider<SavedStateHandle> savedStateProvider;

  public ComplaintThreadViewModel_Factory(Provider<CustomerRepository> repoProvider,
      Provider<SavedStateHandle> savedStateProvider) {
    this.repoProvider = repoProvider;
    this.savedStateProvider = savedStateProvider;
  }

  @Override
  public ComplaintThreadViewModel get() {
    return newInstance(repoProvider.get(), savedStateProvider.get());
  }

  public static ComplaintThreadViewModel_Factory create(Provider<CustomerRepository> repoProvider,
      Provider<SavedStateHandle> savedStateProvider) {
    return new ComplaintThreadViewModel_Factory(repoProvider, savedStateProvider);
  }

  public static ComplaintThreadViewModel newInstance(CustomerRepository repo,
      SavedStateHandle savedState) {
    return new ComplaintThreadViewModel(repo, savedState);
  }
}
