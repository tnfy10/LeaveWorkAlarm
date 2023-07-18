package xyz.myeoru.leaveworkalarm.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.myeoru.leaveworkalarm.component.AutoCheckSwitch
import xyz.myeoru.leaveworkalarm.ui.theme.LeaveWorkAlarmTheme
import xyz.myeoru.leaveworkalarm.viewmodel.MainViewModel

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 20.dp
            )
    ) {
        AutoCheckSwitch()
    }
}

@Preview(showBackground = true)
@Composable
fun MainViewPreview() {
    LeaveWorkAlarmTheme {
        MainScreen()
    }
}