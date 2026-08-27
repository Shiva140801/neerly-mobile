package com.neerly.mobile.feature.review;

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
public final class ReviewSubmitViewModel_Factory implements Factory<ReviewSubmitViewModel> {
  private final Provider<TrustRepository> repoProvider;

  public ReviewSubmitViewModel_Factory(Provider<TrustRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public ReviewSubmitViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static ReviewSubmitViewModel_Factory create(Provider<TrustRepository> repoProvider) {
    return new ReviewSubmitViewModel_Factory(repoProvider);
  }

  public static ReviewSubmitViewModel newInstance(TrustRepository repo) {
    return new ReviewSubmitViewModel(repo);
  }
}
