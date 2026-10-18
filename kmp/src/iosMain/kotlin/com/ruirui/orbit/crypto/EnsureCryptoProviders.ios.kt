package com.ruirui.orbit.crypto

actual fun ensureCryptoProviders() {
    // No-op on iOS (uses native Apple CryptoKit via dev.whyoleg.cryptography)
}
