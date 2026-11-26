package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "Auth"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("تعذّر العثور على إعدادات تسجيل الدخول (default_web_client_id)")
        return
    }
    val activity = context as? Activity
    if (activity == null) {
        onAuthError("تعذّر بدء نافذة تسجيل الدخول")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("نوع بيانات الاعتماد غير متوقع")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "فشل تسجيل الدخول بحساب Google")
        }
    }
}

@Volatile
private var pendingSignUpDisplayName: String? = null

fun consumePendingSignUpDisplayName(): String? {
    val name = pendingSignUpDisplayName
    pendingSignUpDisplayName = null
    return name
}

private fun mapEmailAuthError(e: Exception): String {
    if (e is FirebaseNetworkException) {
        return "تحقق من اتصالك بالإنترنت وحاول مرة أخرى."
    }
    if (e is FirebaseAuthWeakPasswordException) {
        return "كلمة المرور ضعيفة، يجب أن تكون 6 أحرف على الأقل."
    }
    if (e is FirebaseAuthUserCollisionException) {
        return "هذا البريد الإلكتروني مسجل مسبقاً، جرّب تسجيل الدخول."
    }
    if (e is FirebaseAuthInvalidUserException) {
        return "لا يوجد حساب مسجل بهذا البريد الإلكتروني."
    }
    if (e is FirebaseAuthInvalidCredentialsException) {
        return "البريد الإلكتروني أو كلمة المرور غير صحيحة."
    }
    val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
    val rawMessage = e.message.orEmpty()
    return when {
        code == "ERROR_OPERATION_NOT_ALLOWED" ||
            rawMessage.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            rawMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
            "تسجيل البريد الإلكتروني غير مفعّل بعد في لوحة Firebase (Authentication > Sign-in method > Email/Password). يمكنك الدخول فوراً بحساب جوجل."
        code == "ERROR_TOO_MANY_REQUESTS" || rawMessage.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ->
            "محاولات كثيرة جداً، الرجاء الانتظار قليلاً ثم المحاولة مرة أخرى."
        code == "ERROR_INVALID_EMAIL" ->
            "صيغة البريد الإلكتروني غير صحيحة."
        else -> e.localizedMessage ?: "تعذّر إكمال عملية التسجيل بالبريد الإلكتروني."
    }
}

fun signInWithEmail(
    email: String,
    password: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
        onAuthError("الرجاء إدخال بريد إلكتروني صحيح.")
        return
    }
    if (password.length < 6) {
        onAuthError("كلمة المرور يجب أن تكون 6 أحرف على الأقل.")
        return
    }
    scope.launch {
        try {
            Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
            onAuthSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in failed", e)
            onAuthError(mapEmailAuthError(e))
        }
    }
}

fun signUpWithEmail(
    displayName: String,
    email: String,
    password: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
) {
    val cleanName = displayName.trim()
    val cleanEmail = email.trim()
    if (cleanName.length < 2) {
        onAuthError("الرجاء إدخال اسمك في اللعبة (حرفين على الأقل).")
        return
    }
    if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
        onAuthError("الرجاء إدخال بريد إلكتروني صحيح.")
        return
    }
    if (password.length < 6) {
        onAuthError("كلمة المرور يجب أن تكون 6 أحرف على الأقل.")
        return
    }
    pendingSignUpDisplayName = cleanName
    scope.launch {
        try {
            val result = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            result.user?.let { createdUser ->
                runCatching {
                    createdUser.updateProfile(
                        userProfileChangeRequest {
                            this.displayName = cleanName
                        },
                    ).await()
                }
            }
            onAuthSuccess()
        } catch (e: Exception) {
            pendingSignUpDisplayName = null
            Log.e(TAG, "Email sign-up failed", e)
            onAuthError(mapEmailAuthError(e))
        }
    }
}

fun sendPasswordReset(
    email: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope,
) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
        onError("أدخل بريدك الإلكتروني أولاً لإرسال رابط استعادة كلمة المرور.")
        return
    }
    scope.launch {
        try {
            Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
            onSuccess("تم إرسال رابط استعادة كلمة المرور إلى بريدك الإلكتروني.")
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            onError(mapEmailAuthError(e))
        }
    }
}

@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "التسجيل بحساب جوجل",
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    border: BorderStroke? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { msg ->
                    isLoading = false
                    onAuthError(msg)
                },
                scope = scope,
                onAuthCancelled = { isLoading = false },
            )
        },
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_sign_in_button"),
        shape = MaterialTheme.shapes.medium,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = contentColor,
                strokeWidth = 2.5.dp,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Login,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {},
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
