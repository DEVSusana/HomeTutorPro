
package com.devsusana.hometutorpro.presentation.register

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsusana.hometutorpro.presentation.register.components.RegisterContent

/*
Robustness Critique:
1.  **Input Validation**: The email and password fields lack client-side validation. This could lead to unnecessary API calls with invalid data and a poor user experience. Implement checks for email format and password strength.
2.  **Error Display**: The error message from the ViewModel is not displayed to the user. If registration fails, the user will not know why. The UI should show the `state.error` message.
3.  **Authentication Token Handling**: After a successful registration, the user is redirected to the login screen. A better user experience would be to automatically log the user in and navigate them to the main screen of the app, securely handling the authentication token.
*/

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RegisterContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )

    LaunchedEffect(state.registerSuccess) {
        if (state.registerSuccess) {
            onRegisterSuccess()
        }
    }

    if (state.errorMessage != null) {
        com.devsusana.hometutorpro.presentation.components.FeedbackDialog(
            isSuccess = false,
            message = { Text(stringResource(id = state.errorMessage!!)) },
            onDismiss = viewModel::clearFeedback
        )
    }
}
