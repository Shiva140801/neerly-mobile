package com.neerly.mobile.feature.checkout;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class PaymentLauncher_Factory implements Factory<PaymentLauncher> {
  @Override
  public PaymentLauncher get() {
    return newInstance();
  }

  public static PaymentLauncher_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PaymentLauncher newInstance() {
    return new PaymentLauncher();
  }

  private static final class InstanceHolder {
    private static final PaymentLauncher_Factory INSTANCE = new PaymentLauncher_Factory();
  }
}
