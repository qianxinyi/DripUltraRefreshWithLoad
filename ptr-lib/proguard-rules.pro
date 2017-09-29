-printmapping mapping.txt
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable
-keepattributes *Annotation*
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Signature
-dontoptimize
-ignorewarnings

-keepattributes Exceptions,InnerClasses,...

-keep class in.srain.*
-dontwarn android.graphics.Bitmap
