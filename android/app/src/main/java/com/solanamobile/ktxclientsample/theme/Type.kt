package com.solanamobile.ktxclientsample.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.solanamobile.ktxclientsample.R

val PixelFont = FontFamily(
    Font(R.font.press_start_2p, FontWeight.Normal)
)

val Typography = Typography(
    defaultFontFamily = PixelFont,
    h3 = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = 2.sp
    ),
    h4 = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        letterSpacing = 2.sp
    ),
    h5 = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 1.sp
    ),
    h6 = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    ),
    subtitle1 = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    ),
    subtitle2 = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp
    ),
    body1 = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    ),
    body2 = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp
    ),
    button = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.sp
    ),
    caption = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 7.sp
    ),
    overline = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 7.sp,
        letterSpacing = 2.sp
    )
)
