package com.devsusana.hometutorpro.presentation.resources

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen entry point for class resources. Connects [ResourceViewModel] with [ResourcesContent].
 */
@Composable
fun ResourcesScreen(
    viewModel: ResourceViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val defaultNamePrefix = stringResource(R.string.resource_default_name_prefix)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileInfo = com.devsusana.hometutorpro.core.utils.FilePickerHelper.getFileInfo(context, it)
            if (fileInfo != null) {
                viewModel.uploadResource(it, fileInfo.name, fileInfo.type)
            } else {
                val name = "${defaultNamePrefix}${System.currentTimeMillis()}"
                viewModel.uploadResource(it, name, "application/octet-stream")
            }
        }
    }

    ResourcesContent(
        state = state,
        onBack = onBack,
        onUploadClick = { launcher.launch("*/*") },
        onResourceClick = { resource ->
            try {
                val file = java.io.File(Uri.parse(resource.url).path ?: resource.url)
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val mimeType = context.contentResolver.getType(contentUri) ?: "*/*"

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, resource.name))
            } catch (e: Exception) {
                // If sharing fails, show error silently
            }
        },
        onDeleteResource = viewModel::deleteResource
    )

    val successMessage = state.successMessage
    if (successMessage != null) {
        FeedbackDialog(
            isSuccess = true,
            message = {
                when (val message = successMessage) {
                    is Int -> Text(stringResource(id = message))
                    is String -> Text(message)
                }
            },
            onDismiss = viewModel::clearFeedback
        )
    }

    val errorMessage = state.errorMessage
    if (errorMessage != null) {
        FeedbackDialog(
            isSuccess = false,
            message = {
                when (val message = errorMessage) {
                    is Int -> Text(stringResource(id = message))
                    is String -> Text(message)
                }
            },
            onDismiss = viewModel::clearFeedback
        )
    }
}
