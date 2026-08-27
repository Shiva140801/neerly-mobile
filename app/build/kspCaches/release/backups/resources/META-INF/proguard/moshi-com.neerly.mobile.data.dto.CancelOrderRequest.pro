-if class com.neerly.mobile.data.dto.CancelOrderRequest
-keepnames class com.neerly.mobile.data.dto.CancelOrderRequest
-if class com.neerly.mobile.data.dto.CancelOrderRequest
-keep class com.neerly.mobile.data.dto.CancelOrderRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
