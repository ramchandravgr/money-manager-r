package com.rrparallelstudio.miapp.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object MmrDeviceCredentialGate {
    /** When true, [authenticate] invokes [onSuccess] immediately (instrumented UI tests only). */
    @Volatile
    var bypassForInstrumentedTests: Boolean = false

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "Confirm to continue",
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        if (bypassForInstrumentedTests) {
            onSuccess()
            return
        }
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val biometricManager = BiometricManager.from(activity)
        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            onSuccess()
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        onFailure()
                    }

                    override fun onAuthenticationFailed() = Unit
                },
            )
        val info =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(authenticators)
                .build()
        prompt.authenticate(info)
    }
}
