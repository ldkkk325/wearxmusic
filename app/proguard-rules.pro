# Media3 / ExoPlayer
-dontwarn androidx.media3.**

# Keep MediaSessionService + MediaLibraryService implementations (referenced from manifest)
-keep class com.wearx.music.playback.PlaybackService { *; }

# Keep data models used across process boundaries / parcelable-ish state
-keep class com.wearx.music.data.model.** { *; }

# Kotlin metadata
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-dontwarn org.jetbrains.annotations.**
