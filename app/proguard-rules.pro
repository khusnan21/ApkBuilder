# Keep WebView interface
-keepclassmembers class com.nkjayanet.app.WebAppInterface {
    public *;
}

# Keep MainActivity
-keep class com.nkjayanet.app.MainActivity {
    public *;
}

# Keep all app classes
-keep class com.nkjayanet.app.** {
    *;
}

# Keep asset loader and native bridge
-keep class com.nkjayanet.app.util.** {
    *;
}
-keep class com.nkjayanet.app.nativebridge.** {
    *;
}

# Debugging support
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
