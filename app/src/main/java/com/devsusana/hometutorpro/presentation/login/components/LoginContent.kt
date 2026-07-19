package com.devsusana.hometutorpro.presentation.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.login.LoginState
import com.devsusana.hometutorpro.presentation.login.LoginUiEvent
import com.devsusana.hometutorpro.presentation.utils.GoogleSignInHelper
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

@Composable
fun LoginContent(
    state: LoginState,
    onEvent: (LoginUiEvent) -> Unit,
    onRegisterClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .systemBarsPadding()
            .imePadding()
            .testTag("login_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo / Icon
            Image(
                painter = painterResource(id = R.drawable.ic_app_icon),
                contentDescription = stringResource(R.string.cd_app_logo),
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { onEvent(LoginUiEvent.OnEmailChange(it)) },
                        label = { Text(stringResource(R.string.email)) },
                        modifier = Modifier.fillMaxWidth().testTag("email_field"),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = stringResource(R.string.cd_email_icon)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onEvent(LoginUiEvent.OnPasswordChange(it)) },
                        label = { Text(stringResource(R.string.password)) },
                        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.cd_password_icon)) },
                        trailingIcon = {
                            IconButton(onClick = { onEvent(LoginUiEvent.OnTogglePasswordVisibility) }) {
                                Icon(
                                    imageVector = if (state.isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (state.isPasswordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("password_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = stringResource(R.string.login_forgot_password),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .clickable { onEvent(LoginUiEvent.OnForgotPasswordClick) }
                                .padding(vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onEvent(LoginUiEvent.OnLoginClick) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.loading))
                        } else {
                            Icon(Icons.Default.Login, contentDescription = stringResource(R.string.cd_login_icon))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.login))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.or_divider),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            GoogleSignInHelper.launchGoogleSignIn(
                                context = context,
                                scope = scope,
                                onSuccess = { idToken -> onEvent(LoginUiEvent.OnGoogleSignInSuccess(idToken)) },
                                onError = { err -> onEvent(LoginUiEvent.OnGoogleSignInError(err)) }
                            )
                        },
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_login_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = stringResource(R.string.google_sign_in),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.google_sign_in))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = onRegisterClick,
                modifier = Modifier.testTag("register_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.cd_register_icon), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.register))
            }
        }

        if (state.showForgotPasswordDialog) {
            var emailInput by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { onEvent(LoginUiEvent.OnDismissForgotPasswordDialog) },
                title = {
                    Text(
                        text = stringResource(R.string.login_forgot_password_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.login_forgot_password_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text(stringResource(R.string.email)) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (state.error != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(state.error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onEvent(LoginUiEvent.OnSendPasswordResetEmail(emailInput)) },
                        enabled = !state.isSendingPasswordReset && emailInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isSendingPasswordReset) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.login_forgot_password_send))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onEvent(LoginUiEvent.OnDismissForgotPasswordDialog) },
                        enabled = !state.isSendingPasswordReset
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Login Content Default")
@Composable
fun LoginContentPreview() {
    HomeTutorProTheme {
        LoginContent(
            state = LoginState(),
            onEvent = {},
            onRegisterClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Login Content Loading")
@Composable
fun LoginContentLoadingPreview() {
    HomeTutorProTheme {
        LoginContent(
            state = LoginState(isLoading = true),
            onEvent = {},
            onRegisterClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Login Content With Input")
@Composable
fun LoginContentWithInputPreview() {
    HomeTutorProTheme {
        LoginContent(
            state = LoginState(email = "test@example.com", password = "password"),
            onEvent = {},
            onRegisterClick = {}
        )
    }
}
