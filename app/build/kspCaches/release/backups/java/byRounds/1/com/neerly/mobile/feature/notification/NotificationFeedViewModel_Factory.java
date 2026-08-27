package com.neerly.mobile.feature.notification;

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
public final class NotificationFeedViewModel_Factory implements Factory<NotificationFeedViewModel> {
  private final Provider<TrustRepository> repoProvider;

  public NotificationFeedViewModel_Factory(Provider<TrustRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public NotificationFeedViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static NotificationFeedViewModel_Factory create(Provider<TrustRepository> repoProvider) {
    return new NotificationFeedViewModel_Factory(repoProvider);
  }

  public static NotificationFeedViewModel newInstance(TrustRepository repo) {
    return new NotificationFeedViewModel(repo);
  }
}
