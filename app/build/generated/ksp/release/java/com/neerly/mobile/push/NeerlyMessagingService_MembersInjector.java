package com.neerly.mobile.push;

import com.neerly.mobile.data.auth.AuthRepository;
import com.neerly.mobile.data.auth.TokenStore;
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
public final class NeerlyMessagingService_MembersInjector implements MembersInjector<NeerlyMessagingService> {
  private final Provider<AuthRepository> authProvider;

  private final Provider<TokenStore> tokensProvider;

  public NeerlyMessagingService_MembersInjector(Provider<AuthRepository> authProvider,
      Provider<TokenStore> tokensProvider) {
    this.authProvider = authProvider;
    this.tokensProvider = tokensProvider;
  }

  public static MembersInjector<NeerlyMessagingService> create(
      Provider<AuthRepository> authProvider, Provider<TokenStore> tokensProvider) {
    return new NeerlyMessagingService_MembersInjector(authProvider, tokensProvider);
  }

  @Override
  public void injectMembers(NeerlyMessagingService instance) {
    injectAuth(instance, authProvider.get());
    injectTokens(instance, tokensProvider.get());
  }

  @InjectedFieldSignature("com.neerly.mobile.push.NeerlyMessagingService.auth")
  public static void injectAuth(NeerlyMessagingService instance, AuthRepository auth) {
    instance.auth = auth;
  }

  @InjectedFieldSignature("com.neerly.mobile.push.NeerlyMessagingService.tokens")
  public static void injectTokens(NeerlyMessagingService instance, TokenStore tokens) {
    instance.tokens = tokens;
  }
}
