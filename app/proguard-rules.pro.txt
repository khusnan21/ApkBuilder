# Jangan obfuscate WebView interface
-keepclassmembers class com.nkjayanet.app.WebAppInterface {
    public *;
}

# Jangan obfuscate Activity utama
-keep class com.nkjayanet.app.MainActivity {
    public *;
}

# Jangan obfuscate semua class di package utama
-keep class com.nkjayanet.app.** {
    *;
}

# Jangan obfuscate asset loader atau file extractor
-keep class com.nkjayanet.app.util.** {
    *;
}

# Simpan informasi line number untuk debugging
-keepattributes SourceFile,LineNumberTable

# Hindari rename source file name
-renamesourcefileattribute SourceFile

# Jangan obfuscate native loader (jika ada JNI atau binary runner)
-keep class com.nkjayanet.app.nativebridge.** {
    *;
}
