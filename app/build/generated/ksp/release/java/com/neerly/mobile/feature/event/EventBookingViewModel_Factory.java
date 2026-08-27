package com.neerly.mobile.feature.event;

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
public final class EventBookingViewModel_Factory implements Factory<EventBookingViewModel> {
  private final Provider<CustomerRepository> repoProvider;

  public EventBookingViewModel_Factory(Provider<CustomerRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public EventBookingViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static EventBookingViewModel_Factory create(Provider<CustomerRepository> repoProvider) {
    return new EventBookingViewModel_Factory(repoProvider);
  }

  public static EventBookingViewModel newInstance(CustomerRepository repo) {
    return new EventBookingViewModel(repo);
  }
}
