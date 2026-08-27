-if class com.neerly.mobile.data.dto.OrderItemRequest
-keepnames class com.neerly.mobile.data.dto.OrderItemRequest
-if class com.neerly.mobile.data.dto.OrderItemRequest
-keep class com.neerly.mobile.data.dto.OrderItemRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.OrderItemRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.OrderItemRequest
-keepclassmembers class com.neerly.mobile.data.dto.OrderItemRequest {
    public synthetic <init>(java.lang.String,int,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
