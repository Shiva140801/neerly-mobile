package com.neerly.mobile.feature.complaint;

import com.neerly.mobile.data.repo.TrustRepository;
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
public final class ComplaintFileViewModel_Factory implements Factory<ComplaintFileViewModel> {
  private final Provider<TrustRepository> repoProvider;

  public ComplaintFileViewModel_Factory(Provider<TrustRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public ComplaintFileViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static ComplaintFileViewModel_Factory create(Provider<TrustRepository> repoProvider) {
    return new ComplaintFileViewModel_Factory(repoProvider);
  }

  public static ComplaintFileViewModel newInstance(TrustRepository repo) {
    return new ComplaintFileViewModel(repo);
  }
}
