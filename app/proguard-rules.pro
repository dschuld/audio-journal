# Keep AWS SDK for Kotlin service clients working after shrinking.
-keep class aws.sdk.kotlin.** { *; }
-keep class aws.smithy.kotlin.** { *; }
-dontwarn aws.sdk.kotlin.**
-dontwarn aws.smithy.kotlin.**
-dontwarn org.slf4j.**
