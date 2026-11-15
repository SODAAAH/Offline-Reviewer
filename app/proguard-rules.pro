-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.studentreviewer.offline.ai.LlamaBridge {
    native <methods>;
    public <methods>;
}

-keepclassmembers class com.studentreviewer.offline.ai.GgufMetadata {
    <fields>;
}

-keepclassmembers class com.studentreviewer.offline.data.entity.** {
    <fields>;
}

-dontwarn org.xmlpull.v1.**
