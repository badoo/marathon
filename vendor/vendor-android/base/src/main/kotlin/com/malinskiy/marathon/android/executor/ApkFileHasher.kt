package com.malinskiy.marathon.android

import com.android.apksig.ApkVerifier
import com.malinskiy.marathon.io.FileHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.HexFormat

/**
 * Extracts digest of APK contents from signature file
 */
class ApkFileHasher : FileHasher {

    val sha256digest = MessageDigest.getInstance("SHA-256")

    override suspend fun getHash(file: File): String = withContext(Dispatchers.IO) {
        val result = ApkVerifier.Builder(file).build().verify()
        if (result.signerCertificates.isNotEmpty()) {
            val certificate = result.signerCertificates.first()
            // https://cs.android.com/android/platform/superproject/main/+/main:tools/apksig/src/apksigner/java/com/android/apksigner/ApkSignerTool.java;l=1151;drc=d5137445c0d4067406cb3e38aade5507ff2fcd16
            HexFormat.of().formatHex(sha256digest.digest(certificate.encoded))
        } else {
            throw IllegalArgumentException("Certificate not found")
        }
    }
}
