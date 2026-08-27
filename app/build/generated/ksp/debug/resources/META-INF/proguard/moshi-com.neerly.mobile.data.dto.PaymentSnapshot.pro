-if class com.neerly.mobile.data.dto.PaymentSnapshot
-keepnames class com.neerly.mobile.data.dto.PaymentSnapshot
-if class com.neerly.mobile.data.dto.PaymentSnapshot
-keep class com.neerly.mobile.data.dto.PaymentSnapshotJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
