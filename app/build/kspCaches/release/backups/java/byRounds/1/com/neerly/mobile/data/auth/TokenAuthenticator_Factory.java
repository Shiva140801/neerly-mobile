package com.neerly.mobile.data.auth;

import com.neerly.mobile.data.api.NeerlyApi;
import com.squareup.moshi.Moshi;
import dagger.Lazy;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TokenAuthenticator_Factory implements Factory<TokenAuthenticator> {
  private final Provider<TokenStore> tokensProvider;

  private final Provider<NeerlyApi> apiLazyProvider;

  private final Provider<Moshi> moshiProvider;

  public TokenAuthenticator_Factory(Provider<TokenStore> tokensProvider,
      Provider<NeerlyApi> apiLazyProvider, Provider<Moshi> moshiProvider) {
    this.tokensProvider = tokensProvider;
    this.apiLazyProvider = apiLazyProvider;
    this.moshiProvider = moshiProvider;
  }

  @Override
  public TokenAuthenticator get() {
    return newInstance(tokensProvider.get(), DoubleCheck.lazy(apiLazyProvider), moshiProvider.get());
  }

  public static TokenAuthenticator_Factory create(Provider<TokenStore> tokensProvider,
      Provider<NeerlyApi> apiLazyProvider, Provider<Moshi> moshiProvider) {
    return new TokenAuthenticator_Factory(tokensProvider, apiLazyProvider, moshiProvider);
  }

  public static TokenAuthenticator newInstance(TokenStore tokens, Lazy<NeerlyApi> apiLazy,
      Moshi moshi) {
    return new TokenAuthenticator(tokens, apiLazy, moshi);
  }
}
