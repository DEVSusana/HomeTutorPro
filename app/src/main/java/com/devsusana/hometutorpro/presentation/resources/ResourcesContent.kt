package com.devsusana.hometutorpro.presentation.resources

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.domain.entities.Resource
import com.devsusana.hometutorpro.presentation.resources.components.ResourceItem
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure presentation composable for class resources list.
 */
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
        containerColor = MaterialTheme.colorScheme.background,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
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

            val dateFormat = remember {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(state.resources, key = { it.id }) { resource ->
                    ResourceItem(
                        name = resource.name,
                        date = dateFormat.format(resource.uploadDate),
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
