-if class com.neerly.mobile.data.dto.UserSummary
-keepnames class com.neerly.mobile.data.dto.UserSummary
-if class com.neerly.mobile.data.dto.UserSummary
-keep class com.neerly.mobile.data.dto.UserSummaryJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
