import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.example.compose.AppTheme
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

@Composable
fun App() = AppTheme(true) {
    Surface(Modifier.fillMaxSize()) {

        val viewModel = ViewModel.Factory
        val state = viewModel.uiState.collectAsState().value

        Column {
            ConnectionBar()
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Button(onClick = {
                    viewModel.sendMessage("endpoint : DONATE")
                }) {
                    Text("Send Message")
                }
            }
        }
    }
}

@Composable
fun ConnectionBar(modifier: Modifier = Modifier) = NavigationBar {
    val viewModel = ViewModel.Factory
    val state = viewModel.uiState.collectAsState().value

    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(2f).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                modifier = Modifier.padding(5.dp),
                text = if (state.connection != "") "Connected To: ${state.connection}" else "Not Connected...",
                maxLines = 1
            )
            Button(modifier = Modifier.padding(5.dp).fillMaxSize().clip(RoundedCornerShape(5.dp)), onClick = {
                viewModel.closeConnection()
            },
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(5.dp)
            ) {
                Text("Disconnect")
            }
        }

        Row(Modifier.padding(5.dp).weight(4f), verticalAlignment = Alignment.CenterVertically) {
            var text by remember { mutableStateOf("") }
            val interactionSource = remember { MutableInteractionSource() }
            TextField(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .weight(3f),
                value = text,
                onValueChange = {
                    if (it.length < 46) text = it
                },
                placeholder = {Text("Endpoint Ip Address...")},
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                )
            )
            Button(modifier = Modifier.padding(5.dp).fillMaxHeight(), onClick = {
                viewModel.openConnection(text)
                },
                shape = RoundedCornerShape(5.dp)
            ) {
                Text("Connect")
            }

        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GoFundMe Client",
        state = WindowState(size = DpSize(1400.dp,1000.dp))
    ) {
        App()
    }
}
