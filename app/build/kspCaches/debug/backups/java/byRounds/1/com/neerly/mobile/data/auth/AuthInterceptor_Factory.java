package com.neerly.mobile.data.auth;

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
public final class AuthInterceptor_Factory implements Factory<AuthInterceptor> {
  private final Provider<TokenStore> tokensProvider;

  public AuthInterceptor_Factory(Provider<TokenStore> tokensProvider) {
    this.tokensProvider = tokensProvider;
  }

  @Override
  public AuthInterceptor get() {
    return newInstance(tokensProvider.get());
  }

  public static AuthInterceptor_Factory create(Provider<TokenStore> tokensProvider) {
    return new AuthInterceptor_Factory(tokensProvider);
  }

  public static AuthInterceptor newInstance(TokenStore tokens) {
    return new AuthInterceptor(tokens);
  }
}
