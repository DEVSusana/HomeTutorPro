package com.devsusana.hometutorpro.presentation.register.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.devsusana.hometutorpro.presentation.register.RegisterState
import com.devsusana.hometutorpro.presentation.register.RegisterUiEvent
import com.devsusana.hometutorpro.presentation.utils.GoogleSignInHelper
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

@Composable
fun RegisterContent(
    state: RegisterState,
    onEvent: (RegisterUiEvent) -> Unit,
    onBack: () -> Unit
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
            .testTag("register_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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
                text = stringResource(R.string.register_title),
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
                        value = state.name,
                        onValueChange = { onEvent(RegisterUiEvent.OnNameChange(it)) },
                        label = { Text(stringResource(R.string.name)) },
                        modifier = Modifier.fillMaxWidth().testTag("name_field"),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.cd_person_icon)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = state.error == R.string.register_error_name_empty,
                        supportingText = if (state.error == R.string.register_error_name_empty) {
                            { Text(stringResource(R.string.register_error_name_empty)) }
                        } else null
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { onEvent(RegisterUiEvent.OnEmailChange(it)) },
                        label = { Text(stringResource(R.string.email)) },
                        modifier = Modifier.fillMaxWidth().testTag("email_field"),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = stringResource(R.string.cd_email_icon)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = state.error == R.string.register_error_invalid_email,
                        supportingText = if (state.error == R.string.register_error_invalid_email) {
                            { Text(stringResource(R.string.register_error_invalid_email)) }
                        } else null
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onEvent(RegisterUiEvent.OnPasswordChange(it)) },
                        label = { Text(stringResource(R.string.password)) },
                        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.cd_password_icon)) },
                        trailingIcon = {
                            IconButton(onClick = { onEvent(RegisterUiEvent.OnTogglePasswordVisibility) }) {
                                Icon(
                                    imageVector = if (state.isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (state.isPasswordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("password_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = state.error == R.string.register_error_short_password,
                        supportingText = if (state.error == R.string.register_error_short_password) {
                            { Text(stringResource(R.string.register_error_short_password)) }
                        } else null
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { onEvent(RegisterUiEvent.OnRegisterClick) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 50.dp).heightIn(min = 50.dp).testTag("register_button"),
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
                            Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.cd_register_icon))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.register))
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
                                onSuccess = { idToken -> onEvent(RegisterUiEvent.OnGoogleSignInSuccess(idToken)) },
                                onError = { err -> onEvent(RegisterUiEvent.OnGoogleSignInError(err)) }
                            )
                        },
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp)
                            .heightIn(min = 50.dp)
                            .testTag("google_register_button"),
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.register_legal_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Text(
                            text = stringResource(R.string.register_legal_terms),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://hometutorpro.web.app/terms_of_use.html")
                            }
                        )
                        Text(
                            text = "  •  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.register_legal_privacy),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://hometutorpro.web.app/privacy_policy.html")
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back_button), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.back_to_login))
            }
        }
    }
}

@Preview(showBackground = true, name = "Register Content Default")
@Composable
fun RegisterContentPreview() {
    HomeTutorProTheme {
        RegisterContent(
            state = RegisterState(),
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Register Content Loading")
@Composable
fun RegisterContentLoadingPreview() {
    HomeTutorProTheme {
        RegisterContent(
            state = RegisterState(isLoading = true),
            onEvent = {},
            onBack = {}
        )
    }
}
