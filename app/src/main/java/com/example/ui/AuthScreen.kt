package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleSignInButton
import com.example.auth.attemptAutoSignIn
import com.example.auth.sendPasswordReset
import com.example.auth.signInWithEmail
import com.example.auth.signUpWithEmail
import com.example.ui.components.YellowWavePatternBackground
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.ShaddahaBlue
import com.example.ui.theme.ShaddahaBlueSoft
import com.example.ui.theme.ShaddahaSurfaceSoft
import com.example.ui.theme.ShaddahaWhite
import com.example.ui.theme.ShaddahaYellow
import com.example.ui.theme.TealAccent

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isSignUpMode by rememberSaveable { mutableStateOf(false) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isEmailLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope,
        )
    }

    val submitEmailAuth: () -> Unit = {
        focusManager.clearFocus()
        errorMessage = null
        infoMessage = null
        isEmailLoading = true
        if (isSignUpMode) {
            signUpWithEmail(
                displayName = displayName,
                email = email,
                password = password,
                onAuthSuccess = {
                    isEmailLoading = false
                    onAuthSuccess()
                },
                onAuthError = { msg ->
                    isEmailLoading = false
                    errorMessage = msg
                },
                scope = scope,
            )
        } else {
            signInWithEmail(
                email = email,
                password = password,
                onAuthSuccess = {
                    isEmailLoading = false
                    onAuthSuccess()
                },
                onAuthError = { msg ->
                    isEmailLoading = false
                    errorMessage = msg
                },
                scope = scope,
            )
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ShaddahaBlue,
        unfocusedBorderColor = OutlineSoft,
        focusedContainerColor = ShaddahaWhite,
        unfocusedContainerColor = ShaddahaSurfaceSoft,
        focusedLabelColor = ShaddahaBlue,
        unfocusedLabelColor = InkMuted,
        focusedTextColor = InkDark,
        unfocusedTextColor = InkDark,
        cursorColor = ShaddahaBlue,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShaddahaWhite)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            Image(
                painter = painterResource(id = R.drawable.shaddaha_logo),
                contentDescription = "شعار شدها",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(172.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "لعبة بلوت .. تفهــــــمك",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = ShaddahaBlue,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Toggle: Sign In vs Create Account
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ShaddahaSurfaceSoft,
                border = BorderStroke(1.dp, OutlineSoft),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AuthModeTab(
                        label = "تسجيل الدخول",
                        selected = !isSignUpMode,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_mode_signin_tab"),
                        onClick = {
                            isSignUpMode = false
                            errorMessage = null
                            infoMessage = null
                        },
                    )
                    AuthModeTab(
                        label = "حساب جديد",
                        selected = isSignUpMode,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_mode_signup_tab"),
                        onClick = {
                            isSignUpMode = true
                            errorMessage = null
                            infoMessage = null
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (!infoMessage.isNullOrBlank()) {
                Surface(
                    color = TealAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Text(
                        text = infoMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = InkDark,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Email & Password Registration / Sign-In Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ShaddahaWhite,
                border = BorderStroke(1.dp, OutlineSoft),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AnimatedVisibility(visible = isSignUpMode) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("الاسم في اللعبة") },
                            placeholder = { Text("مثال: أبو فهد") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "الاسم في اللعبة",
                                    tint = ShaddahaBlue,
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input"),
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        placeholder = { Text("name@example.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Email,
                                contentDescription = "البريد الإلكتروني",
                                tint = ShaddahaBlue,
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        placeholder = { Text("6 أحرف على الأقل") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "كلمة المرور",
                                tint = ShaddahaBlue,
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.testTag("auth_password_visibility_toggle"),
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                    tint = InkMuted,
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (!isEmailLoading) submitEmailAuth() },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                    )

                    if (!isSignUpMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    sendPasswordReset(
                                        email = email,
                                        onSuccess = { msg -> infoMessage = msg },
                                        onError = { msg -> errorMessage = msg },
                                        scope = scope,
                                    )
                                },
                                modifier = Modifier.testTag("auth_forgot_password_button"),
                            ) {
                                Text(
                                    text = "نسيت كلمة المرور؟",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ShaddahaBlue,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = submitEmailAuth,
                        enabled = !isEmailLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShaddahaBlue,
                            contentColor = ShaddahaWhite,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("email_auth_submit_button"),
                    ) {
                        if (isEmailLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = ShaddahaWhite,
                                strokeWidth = 2.5.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Email,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSignUpMode) {
                                    "إنشاء حساب بالبريد الإلكتروني"
                                } else {
                                    "تسجيل الدخول بالبريد الإلكتروني"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Bottom Curved Yellow Wave Section: Google Sign-In + Phone Sign-In (Coming Soon)
        Surface(
            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
            color = ShaddahaYellow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                YellowWavePatternBackground(modifier = Modifier.matchParentSize())
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = InkDark.copy(alpha = 0.22f),
                        )
                        Text(
                            text = "أو التسجيل السريع عبر",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = InkDark.copy(alpha = 0.8f),
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = InkDark.copy(alpha = 0.22f),
                        )
                    }

                    // 1. Google Sign-In (Active via Jetpack Credential Manager)
                    GoogleSignInButton(
                        label = "التسجيل بحساب جوجل",
                        containerColor = ShaddahaWhite,
                        contentColor = ShaddahaBlue,
                        border = BorderStroke(1.2.dp, ShaddahaBlue.copy(alpha = 0.25f)),
                        onAuthSuccess = {
                            errorMessage = null
                            infoMessage = null
                            onAuthSuccess()
                        },
                        onAuthError = { msg -> errorMessage = msg },
                    )

                    // 2. Phone Number Sign-In (Coming Soon — clearly marked non-interactive)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ShaddahaWhite.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, InkDark.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("phone_auth_coming_soon_badge"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneAndroid,
                                    contentDescription = "التسجيل برقم الجوال",
                                    tint = InkDark.copy(alpha = 0.55f),
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "التسجيل برقم الجوال",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = InkDark.copy(alpha = 0.65f),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ShaddahaBlueSoft,
                                border = BorderStroke(1.dp, ShaddahaBlue.copy(alpha = 0.25f)),
                            ) {
                                Text(
                                    text = "قريباً",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = ShaddahaBlue,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = "تسجيل دخول آمن لحفظ نقاطك وتصنيفك عبر جميع أجهزتك",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = InkDark.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthModeTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) ShaddahaBlue else Color.Transparent,
        modifier = modifier.height(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (selected) ShaddahaWhite else InkMuted,
            )
        }
    }
}
