package com.devsusana.hometutorpro.presentation.components.schedule_exception

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils
import java.time.DayOfWeek

/**
 * Dropdown selector for picking a target day of the week when rescheduling a class.
 *
 * @param selectedDay The currently selected day of the week.
 * @param onDaySelected Callback invoked when the user selects a day of the week.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOfWeekDropdownSelector(
    selectedDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = DayOfWeekUtils.getLocalizedName(selectedDay),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.schedule_exception_day_of_week)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DayOfWeek.entries.forEach { day ->
                DropdownMenuItem(
                    text = { Text(DayOfWeekUtils.getLocalizedName(day)) },
                    onClick = {
                        onDaySelected(day)
                        expanded = false
                    }
                )
            }
        }
    }
}
