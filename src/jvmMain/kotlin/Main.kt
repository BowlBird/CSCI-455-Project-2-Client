import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@Composable
@Preview
fun App() = MaterialTheme {
    val viewModel = ViewModel.Factory()
    val state = viewModel.uiState.collectAsState()

    Button(onClick = {
        viewModel.updateUiState(
            state.value.copy(
                text = "This Works!"
            )
        )
    }) {
        Text(text = if (state.value.text == "") "First" else state.value.text )
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
