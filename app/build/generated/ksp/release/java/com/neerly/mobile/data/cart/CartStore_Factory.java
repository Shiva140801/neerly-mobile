package com.neerly.mobile.data.cart;

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
public final class CartStore_Factory implements Factory<CartStore> {
  @Override
  public CartStore get() {
    return newInstance();
  }

  public static CartStore_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CartStore newInstance() {
    return new CartStore();
  }

  private static final class InstanceHolder {
    private static final CartStore_Factory INSTANCE = new CartStore_Factory();
  }
}
