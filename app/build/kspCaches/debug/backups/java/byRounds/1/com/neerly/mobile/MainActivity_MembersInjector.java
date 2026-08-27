package com.neerly.mobile;

import com.neerly.mobile.data.auth.TokenStore;
import com.neerly.mobile.data.cart.CartStore;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<CartStore> cartStoreProvider;

  private final Provider<TokenStore> tokenStoreProvider;

  public MainActivity_MembersInjector(Provider<CartStore> cartStoreProvider,
      Provider<TokenStore> tokenStoreProvider) {
    this.cartStoreProvider = cartStoreProvider;
    this.tokenStoreProvider = tokenStoreProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<CartStore> cartStoreProvider,
      Provider<TokenStore> tokenStoreProvider) {
    return new MainActivity_MembersInjector(cartStoreProvider, tokenStoreProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectCartStore(instance, cartStoreProvider.get());
    injectTokenStore(instance, tokenStoreProvider.get());
  }

  @InjectedFieldSignature("com.neerly.mobile.MainActivity.cartStore")
  public static void injectCartStore(MainActivity instance, CartStore cartStore) {
    instance.cartStore = cartStore;
  }

  @InjectedFieldSignature("com.neerly.mobile.MainActivity.tokenStore")
  public static void injectTokenStore(MainActivity instance, TokenStore tokenStore) {
    instance.tokenStore = tokenStore;
  }
}
