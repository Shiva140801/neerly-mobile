-if class com.neerly.mobile.data.dto.WalletTransaction
-keepnames class com.neerly.mobile.data.dto.WalletTransaction
-if class com.neerly.mobile.data.dto.WalletTransaction
-keep class com.neerly.mobile.data.dto.WalletTransactionJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
