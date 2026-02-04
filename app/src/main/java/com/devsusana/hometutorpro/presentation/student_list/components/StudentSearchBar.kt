package com.devsusana.hometutorpro.presentation.student_list.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.student_list.StudentSortOption

@Composable
fun StudentSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    currentSortOption: StudentSortOption,
    onSortOptionChange: (StudentSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .testTag("search_bar"),
            placeholder = { Text(stringResource(R.string.student_list_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.student_list_cd_search)) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.student_list_cd_clear_search))
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Sort Button
        var showSortMenu by remember { mutableStateOf(false) }
        Box {
            IconButton(
                onClick = { showSortMenu = true },
                modifier = Modifier.testTag("sort_button")
            ) {
                Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.student_list_cd_sort))
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                StudentSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { 
                            Text(when(option) {
                                StudentSortOption.NAME -> stringResource(R.string.student_list_sort_name)
                                StudentSortOption.BALANCE -> stringResource(R.string.student_list_sort_balance)
                                StudentSortOption.LAST_CLASS -> stringResource(R.string.student_list_sort_last_class)
                            })
                        },
                        onClick = {
                            onSortOptionChange(option)
                            showSortMenu = false
                        },
                        leadingIcon = if (currentSortOption == option) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentSearchBarPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        StudentSearchBar(
            query = "",
            onQueryChange = {},
            currentSortOption = StudentSortOption.NAME,
            onSortOptionChange = {}
        )
    }
}
