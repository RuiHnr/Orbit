package com.ruirui.orbit.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

actual fun ensureCryptoProviders() {
    if (Security.getProvider("BC") !is BouncyCastleProvider) {
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}
