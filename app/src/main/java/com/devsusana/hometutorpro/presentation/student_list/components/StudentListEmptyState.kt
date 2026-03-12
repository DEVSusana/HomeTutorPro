package com.devsusana.hometutorpro.presentation.student_list.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.student_list.StudentFilter

@Composable
fun StudentListEmptyState(
    isSearchActive: Boolean,
    selectedFilter: StudentFilter,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when {
                isSearchActive -> Icons.Default.SearchOff
                selectedFilter == StudentFilter.INACTIVE -> Icons.Default.PersonOff
                else -> Icons.Default.PeopleOutline
            },
            contentDescription = when {
                isSearchActive -> stringResource(R.string.cd_no_results_icon)
                selectedFilter == StudentFilter.INACTIVE -> null
                else -> stringResource(R.string.cd_no_students_icon)
            },
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when {
                isSearchActive -> stringResource(R.string.student_list_no_results)
                selectedFilter == StudentFilter.INACTIVE -> stringResource(R.string.student_list_empty_inactive_title)
                else -> stringResource(R.string.student_list_no_students)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        if (!isSearchActive && selectedFilter == StudentFilter.INACTIVE) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.student_list_empty_inactive_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (isSearchActive) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onClearSearch) {
                Text(stringResource(R.string.student_list_clear_search))
            }
        }
    }
}

@Preview(showBackground = true, name = "Empty State - No Search")
@Composable
fun EmptyStateNoSearchPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        StudentListEmptyState(
            isSearchActive = false,
            selectedFilter = StudentFilter.ALL,
            onClearSearch = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty State - Search Active")
@Composable
fun EmptyStateSearchPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        StudentListEmptyState(
            isSearchActive = true,
            selectedFilter = StudentFilter.ALL,
            onClearSearch = {}
        )
    }
}
