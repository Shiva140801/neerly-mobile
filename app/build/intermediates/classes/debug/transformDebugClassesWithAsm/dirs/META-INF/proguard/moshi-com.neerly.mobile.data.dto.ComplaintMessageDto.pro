-if class com.neerly.mobile.data.dto.ComplaintMessageDto
-keepnames class com.neerly.mobile.data.dto.ComplaintMessageDto
-if class com.neerly.mobile.data.dto.ComplaintMessageDto
-keep class com.neerly.mobile.data.dto.ComplaintMessageDtoJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.neerly.mobile.data.dto.ComplaintMessageDto
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.neerly.mobile.data.dto.ComplaintMessageDto
-keepclassmembers class com.neerly.mobile.data.dto.ComplaintMessageDto {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.util.List,boolean,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
