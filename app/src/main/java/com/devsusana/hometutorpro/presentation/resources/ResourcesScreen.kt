package com.devsusana.hometutorpro.presentation.resources

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import java.text.SimpleDateFormat
import java.util.Locale
import com.devsusana.hometutorpro.presentation.resources.components.ResourceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    viewModel: ResourceViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.resources)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.resources_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launcher.launch("*/*") },
                modifier = Modifier.testTag("upload_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.resources_upload_file))
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(state.resources) { resource ->
                    ResourceItem(
                        name = resource.name,
                        date = SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(resource.uploadDate),
                        onClick = {
                            try {
                                val file = java.io.File(Uri.parse(resource.url).path ?: resource.url)
                                val contentUri = androidx.core.content.FileProvider.getUriForFile(
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
                        onDelete = { viewModel.deleteResource(resource.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    if (state.successMessage != null) {
        FeedbackDialog(
            isSuccess = true,
            message = {
                when (val message = state.successMessage) {
                    is Int -> Text(stringResource(id = message))
                    is String -> Text(message)
                }
            },
            onDismiss = viewModel::clearFeedback
        )
    }

    if (state.errorMessage != null) {
        FeedbackDialog(
            isSuccess = false,
            message = {
                when (val message = state.errorMessage) {
                    is Int -> Text(stringResource(id = message))
                    is String -> Text(message)
                }
            },
            onDismiss = viewModel::clearFeedback
        )
    }
}






