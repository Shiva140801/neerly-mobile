package com.neerly.mobile.feature.notification;

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
public final class NotificationPrefsViewModel_Factory implements Factory<NotificationPrefsViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  public NotificationPrefsViewModel_Factory(Provider<CustomerRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public NotificationPrefsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static NotificationPrefsViewModel_Factory create(
      Provider<CustomerRepository> repoProvider) {
    return new NotificationPrefsViewModel_Factory(repoProvider);
  }

  public static NotificationPrefsViewModel newInstance(CustomerRepository repo) {
    return new NotificationPrefsViewModel(repo);
  }
}
