package com.neerly.mobile.data.auth;

import com.neerly.mobile.data.api.NeerlyApi;
import dagger.internal.DaggerGenerated;
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
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<NeerlyApi> apiProvider;

  private final Provider<TokenStore> tokensProvider;

  public AuthRepository_Factory(Provider<NeerlyApi> apiProvider,
      Provider<TokenStore> tokensProvider) {
    this.apiProvider = apiProvider;
    this.tokensProvider = tokensProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(apiProvider.get(), tokensProvider.get());
  }

  public static AuthRepository_Factory create(Provider<NeerlyApi> apiProvider,
      Provider<TokenStore> tokensProvider) {
    return new AuthRepository_Factory(apiProvider, tokensProvider);
  }

  public static AuthRepository newInstance(NeerlyApi api, TokenStore tokens) {
    return new AuthRepository(api, tokens);
  }
}
