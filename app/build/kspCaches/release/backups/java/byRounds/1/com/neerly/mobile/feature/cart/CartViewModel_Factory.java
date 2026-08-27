package com.neerly.mobile.feature.cart;

import com.neerly.mobile.data.api.NeerlyApi;
import com.neerly.mobile.data.cart.CartStore;
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
public final class CartViewModel_Factory implements Factory<CartViewModel> {
  private final Provider<CartStore> storeProvider;

  private final Provider<NeerlyApi> apiProvider;

  public CartViewModel_Factory(Provider<CartStore> storeProvider, Provider<NeerlyApi> apiProvider) {
    this.storeProvider = storeProvider;
    this.apiProvider = apiProvider;
  }

  @Override
  public CartViewModel get() {
    return newInstance(storeProvider.get(), apiProvider.get());
  }

  public static CartViewModel_Factory create(Provider<CartStore> storeProvider,
      Provider<NeerlyApi> apiProvider) {
    return new CartViewModel_Factory(storeProvider, apiProvider);
  }

  public static CartViewModel newInstance(CartStore store, NeerlyApi api) {
    return new CartViewModel(store, api);
  }
}
