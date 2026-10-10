package com.xunji.flashcards.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
val XunjiTypography=Typography(
 displaySmall=TextStyle(fontFamily=FontFamily.Serif,fontSize=40.sp,lineHeight=48.sp,fontWeight=FontWeight.Medium),
 headlineLarge=TextStyle(fontSize=28.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold),
 headlineMedium=TextStyle(fontSize=22.sp,lineHeight=32.sp,fontWeight=FontWeight.SemiBold),
 titleLarge=TextStyle(fontSize=20.sp,lineHeight=30.sp,fontWeight=FontWeight.SemiBold),
 titleMedium=TextStyle(fontSize=17.sp,lineHeight=26.sp,fontWeight=FontWeight.SemiBold),
 bodyLarge=TextStyle(fontSize=17.sp,lineHeight=29.sp),
 bodyMedium=TextStyle(fontSize=15.sp,lineHeight=25.sp),
 labelLarge=TextStyle(fontSize=15.sp,lineHeight=22.sp,fontWeight=FontWeight.SemiBold),
 labelMedium=TextStyle(fontSize=12.sp,lineHeight=18.sp,fontWeight=FontWeight.Medium,letterSpacing=0.5.sp)
)
