package com.devsusana.hometutorpro.presentation.resources

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.Resource
import com.devsusana.hometutorpro.presentation.components.FeedbackDialog
import com.devsusana.hometutorpro.presentation.resources.components.ResourceItem
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesContent(
    state: ResourcesState,
    onBack: () -> Unit,
    onUploadClick: () -> Unit,
    onResourceClick: (Resource) -> Unit,
    onDeleteResource: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.resources),
                        modifier = Modifier.semantics { heading() }
                    )
                },
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
                onClick = onUploadClick,
                modifier = Modifier.testTag("upload_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.resources_upload_file))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
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
                        onClick = { onResourceClick(resource) },
                        onDelete = { onDeleteResource(resource.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Resources Content")
@Composable
private fun ResourcesContentPreview() {
    val sampleResources = listOf(
        Resource(
            id = "res1",
            professorId = "prof1",
            name = "Worksheet.pdf",
            url = "file:///tmp/worksheet.pdf",
            type = "application/pdf",
            uploadDate = Date()
        )
    )

    HomeTutorProTheme {
        ResourcesContent(
            state = ResourcesState(resources = sampleResources, isLoading = false),
            onBack = {},
            onUploadClick = {},
            onResourceClick = {},
            onDeleteResource = {}
        )
    }
}
