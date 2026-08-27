-if class com.neerly.mobile.data.dto.PlaceOrderRequest
-keepnames class com.neerly.mobile.data.dto.PlaceOrderRequest
-if class com.neerly.mobile.data.dto.PlaceOrderRequest
-keep class com.neerly.mobile.data.dto.PlaceOrderRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.PlaceOrderRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.PlaceOrderRequest
-keepclassmembers class com.neerly.mobile.data.dto.PlaceOrderRequest {
    public synthetic <init>(java.lang.String,java.lang.String,java.util.List,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
