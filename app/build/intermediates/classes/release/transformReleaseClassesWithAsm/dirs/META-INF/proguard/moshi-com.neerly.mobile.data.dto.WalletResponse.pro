-if class com.neerly.mobile.data.dto.WalletResponse
-keepnames class com.neerly.mobile.data.dto.WalletResponse
-if class com.neerly.mobile.data.dto.WalletResponse
-keep class com.neerly.mobile.data.dto.WalletResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
