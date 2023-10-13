import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.concurrent.thread

@Composable
@Preview
fun App() = MaterialTheme {
    val viewModel = ViewModel.Factory
    val state = viewModel.uiState.collectAsState().value

    Column {
        repeat(state.messages.size) {
            Text(state.messages[it])
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
