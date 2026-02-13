package org.ntqqrev.yogurt

import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.android.AndroidAppInfo
import org.ntqqrev.acidify.common.android.AndroidAppInfo.Sig
import org.ntqqrev.acidify.common.android.AndroidAppInfo.WtLoginSdkInfo


val bundledPCAppInfo = mapOf(
    "Linux/39038" to AppInfo(
        os = "Linux",
        kernel = "Linux",
        vendorOs = "linux",
        currentVersion = "3.2.19-39038",
        miscBitmap = 32764,
        ptVersion = "2.0.0",
        ssoVersion = 19,
        packageName = "com.tencent.qq",
        wtLoginSdk = "nt.wtlogin.0.0.1",
        appId = 1600001615,
        subAppId = 537313942,
        appClientVersion = 39038,
        mainSigMap = 169742560,
        subSigMap = 0,
        ntLoginType = 1
    ),
)

val bundledAndroidAppInfo = mapOf(
    "AndroidPhone/9.1.60" to AndroidAppInfo(
        os = "Android",
        vendorOs = "android",
        qua = "V1_AND_SQ_9.1.60_9388_YYB_D",
        currentVersion = "9.1.60.045f5d19",
        ptVersion = "9.1.60",
        ssoVersion = 22,
        packageName = "com.tencent.mobileqq",
        apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
        sdkInfo = WtLoginSdkInfo(
            sdkBuildTime = 1740483688,
            sdkVersion = "6.0.0.2568",
            miscBitMap = 150470524,
            subSigMap = 66560,
            mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                    or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                    or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                    or 65536L
        ),
        appId = 16,
        subAppId = 537275636,
        appClientVersion = 0
    ),
    "AndroidPad/9.1.60" to AndroidAppInfo(
        os = "ANDROID",
        vendorOs = "android",
        qua = "V1_AND_SQ_9.1.60_9388_YYB_D",
        currentVersion = "9.1.60.045f5d19",
        ptVersion = "9.1.60",
        ssoVersion = 22,
        packageName = "com.tencent.mobileqq",
        apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
        sdkInfo = WtLoginSdkInfo(
            sdkBuildTime = 1740483688,
            sdkVersion = "6.0.0.2568",
            miscBitMap = 150470524,
            subSigMap = 66560,
            mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                    or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                    or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                    or 65536L
        ),
        appId = 16,
        subAppId = 537275675,
        appClientVersion = 0
    ),
    "AndroidPhone/9.1.70" to AndroidAppInfo(
        os = "Android",
        vendorOs = "android",
        qua = "V1_AND_SQ_9.1.70_9898_YYB_D",
        currentVersion = "9.1.70.88c475c0",
        ptVersion = "9.1.70",
        ssoVersion = 22,
        packageName = "com.tencent.mobileqq",
        apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
        sdkInfo = WtLoginSdkInfo(
            sdkBuildTime = 1745224715,
            sdkVersion = "6.0.0.2574",
            miscBitMap = 150470524,
            subSigMap = 66560,
            mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                    or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                    or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                    or 65536L
        ),
        appId = 16,
        subAppId = 537285947,
        appClientVersion = 0
    ),
    "AndroidPad/9.1.70" to AndroidAppInfo(
        os = "ANDROID",
        vendorOs = "android",
        qua = "V1_AND_SQ_9.1.70_9898_YYB_D",
        currentVersion = "9.1.70.88c475c0",
        ptVersion = "9.1.70",
        ssoVersion = 22,
        packageName = "com.tencent.mobileqq",
        apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
        sdkInfo = WtLoginSdkInfo(
            sdkBuildTime = 1745224715,
            sdkVersion = "6.0.0.2574",
            miscBitMap = 150470524,
            subSigMap = 66560,
            mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                    or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                    or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                    or 65536L
        ),
        appId = 16,
        subAppId = 537285986,
        appClientVersion = 0
    ),
    "AndroidPhone/9.2.0" to AndroidAppInfo(
        os = "Android",
        vendorOs = "android",
        qua = "V1_AND_SQ_9.2.0_10970_YYB_D",
        currentVersion = "9.2.0.0a244d9a",
        ptVersion = "9.2.0",
        ssoVersion = 22,
        packageName = "com.tencent.mobileqq",
        apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
        sdkInfo = WtLoginSdkInfo(
            sdkBuildTime = 1751448568,
            sdkVersion = "6.0.0.2584",
            miscBitMap = 150470524,
            subSigMap = 66560,
            mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                    or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                    or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                    or 65536L
        ),
        appId = 16,
        subAppId = 537303052,
        appClientVersion = 0
    ),
    "AndroidPad/9.2.0" to AndroidAppInfo(
        os = "ANDROID",
        vendorOs = "android",
        qua = "V1_AND_SQ_9.2.0_10970_YYB_D",
        currentVersion = "9.2.0.0a244d9a",
        ptVersion = "9.2.0",
        ssoVersion = 22,
        packageName = "com.tencent.mobileqq",
        apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
        sdkInfo = WtLoginSdkInfo(
            sdkBuildTime = 1751448568,
            sdkVersion = "6.0.0.2584",
            miscBitMap = 150470524,
            subSigMap = 66560,
            mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                    or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                    or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                    or 65536L
        ),
        appId = 16,
        subAppId = 537303091,
        appClientVersion = 0
    ),
    "AndroidPhone/9.2.20" to AndroidAppInfo(
        os = "Android",
        vendorOs = "android",
        qua = "V1_AND_SQ_9.2.20_11650_YYB_D",
        currentVersion = "9.2.20.777b5929",
        ptVersion = "9.2.20",
        ssoVersion = 22,
        packageName = "com.tencent.mobileqq",
        apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
        sdkInfo = WtLoginSdkInfo(
            sdkBuildTime = 1757058014,
            sdkVersion = "6.0.0.2589",
            miscBitMap = 150470524,
            subSigMap = 66560,
            mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                    or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                    or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                    or 65536L
        ),
        appId = 16,
        subAppId = 537315786,
        appClientVersion = 0
    ),
    "AndroidPad/9.2.20" to AndroidAppInfo(
        os = "ANDROID",
        vendorOs = "android",
        qua = "V1_AND_SQ_9.2.20_11650_YYB_D",
        currentVersion = "9.2.20.777b5929",
        ptVersion = "9.2.20",
        ssoVersion = 22,
        packageName = "com.tencent.mobileqq",
        apkSignatureMd5 = "a6b745bf24a2c277527716f6f36eb68d".hexToByteArray(),
        sdkInfo = WtLoginSdkInfo(
            sdkBuildTime = 1757058014,
            sdkVersion = "6.0.0.2589",
            miscBitMap = 150470524,
            subSigMap = 66560,
            mainSigMap = Sig.WLOGIN_A5 or Sig.WLOGIN_RESERVED or Sig.WLOGIN_STWEB or Sig.WLOGIN_A2 or Sig.WLOGIN_ST
                    or Sig.WLOGIN_LSKEY or Sig.WLOGIN_SKEY or Sig.WLOGIN_SIG64 or Sig.WLOGIN_VKEY or Sig.WLOGIN_D2
                    or Sig.WLOGIN_SID or Sig.WLOGIN_PSKEY or Sig.WLOGIN_AQSIG or Sig.WLOGIN_LHSIG or Sig.WLOGIN_PAYTOKEN
                    or 65536L
        ),
        appId = 16,
        subAppId = 537315825,
        appClientVersion = 0
    ),
)