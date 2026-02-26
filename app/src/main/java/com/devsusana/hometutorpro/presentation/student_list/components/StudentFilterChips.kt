package com.devsusana.hometutorpro.presentation.student_list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.student_list.StudentFilter

@Composable
fun StudentFilterChips(
    selectedFilter: StudentFilter,
    onFilterChange: (StudentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(bottom = 8.dp)
    ) {
        items(StudentFilter.entries) { filter ->
            val selectedStateDescription = stringResource(R.string.cd_state_selected)
            val notSelectedStateDescription = stringResource(R.string.cd_state_not_selected)
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(when(filter) {
                        StudentFilter.ALL -> stringResource(R.string.student_list_filter_all)
                        StudentFilter.WITH_BALANCE -> stringResource(R.string.student_list_filter_balance)
                        StudentFilter.ACTIVE -> stringResource(R.string.student_list_filter_active)
                        StudentFilter.INACTIVE -> stringResource(R.string.student_list_filter_inactive)
                    })
                },
                modifier = Modifier.semantics {
                    stateDescription = if (selectedFilter == filter) {
                        selectedStateDescription
                    } else {
                        notSelectedStateDescription
                    }
                },
                leadingIcon = if (selectedFilter == filter) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentFilterChipsPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        StudentFilterChips(
            selectedFilter = StudentFilter.ALL,
            onFilterChange = {}
        )
    }
}
