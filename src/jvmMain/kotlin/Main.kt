import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.example.compose.AppTheme
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import java.sql.Date
import java.time.LocalDate

@Composable
fun App() = AppTheme(true) {
    Surface(Modifier.fillMaxSize(), color = Color(red = 11, blue = 11, green = 11)) {

        val viewModel = ViewModel.Factory
        val state = viewModel.uiState.collectAsState().value

        Column {
            ConnectionBar()
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier
                    .padding(5.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var name by remember { mutableStateOf("") }
                    var amount by remember { mutableStateOf("")}
                    var deadline by remember {mutableStateOf("")}

                    Button(modifier = Modifier.fillMaxWidth().padding(5.dp), onClick = {
                        try {
                            LocalDate.parse(deadline)
                            viewModel.sendMessage(
                                "endpoint : CREATE\n" +
                                "name : $name\n" +
                                "targetAmount : ${amount.toDouble()}\n" +
                                "deadline : $deadline"
                            )
                            name = ""
                            amount = ""
                            deadline = ""
                        } catch (e: Exception) {
                            println("Failed to parse Input!")
                        }
                    }, shape = RoundedCornerShape(5.dp)) {
                        Text("Create New Fundraiser")
                    }
                    Row(Modifier.fillMaxWidth()) {
                        CustomTextField(Modifier.weight(1f), name, {name = it}, {Text("Funraiser Name...")})
                        CustomTextField(Modifier.weight(1f), amount, {amount = it}, {Text("Target Amount...")})
                        CustomTextField(Modifier.weight(1f), deadline, {deadline = it}, {Text("Deadline (YYYY-MM-DD)...")})
                    }
                }

                Row(Modifier.fillMaxSize()) {
                    Column(Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                        .scrollable(rememberScrollState(), orientation = Orientation.Vertical),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Current Fundraisers", color = MaterialTheme.colorScheme.onBackground)
                        repeat(state.currentFundraisers.size) {
                            val fundraiser = state.currentFundraisers[it]
                            Row(modifier = Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(modifier = Modifier.padding(5.dp), text = (it + 1).toString(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                Fundraiser(fundraiser = fundraiser)
                            }
                        }
                    }
                    Column(Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                        .scrollable(rememberScrollState(), orientation = Orientation.Vertical),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Old Fundraisers", color = MaterialTheme.colorScheme.onBackground)
                        repeat(state.oldFundraisers.size) {
                            val fundraiser = state.oldFundraisers[it]
                            Row(modifier = Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(modifier = Modifier.padding(5.dp), text = (it + 1).toString(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                Fundraiser(fundraiser = fundraiser)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Fundraiser(modifier: Modifier = Modifier, fundraiser: Fundraiser) {
    var opened by remember { mutableStateOf(false) }
    Column(modifier
        .fillMaxWidth()
        .padding(5.dp)
        .clip(RoundedCornerShape(5.dp))
        .background(MaterialTheme.colorScheme.secondary)
        .clickable{ opened = !opened},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(fundraiser.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        LinearProgressIndicator((fundraiser.balance/fundraiser.goal).toFloat(), Modifier
            .fillMaxWidth()
            .padding(5.dp, 0.dp)
            .clip(
                RoundedCornerShape(5.dp)
            ),
            MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.background)
        Text("$${String.format("%,.2f", fundraiser.balance)} raised out of $${String.format("%,.2f", fundraiser.goal)}")
        Text(fundraiser.deadline)
        if (opened) {
            var amount by remember { mutableStateOf("") }
            CustomTextField(modifier = Modifier.fillMaxWidth(), value = amount, onValueChange = {amount = it}, placeholder = {Text("Amount To Donate...")})
            Button(onClick = {

                val viewModel = ViewModel.Factory

                if(amount.toDoubleOrNull() != null) {
                    val doubleAmount = amount.toDouble()
                    viewModel.sendMessage(
                        "endpoint : DONATE\nid : ${fundraiser.id}\namount : $doubleAmount"
                    )
                }
            }) {
                Text("Donate!")
            }
        }
    }
}


@Composable
fun CustomTextField(modifier: Modifier = Modifier, value: String, onValueChange: (String) -> Unit, placeholder: @Composable () -> Unit) = TextField(
    modifier = modifier
        .padding(5.dp)
        .clip(RoundedCornerShape(5.dp)),
    value = value,
    onValueChange = onValueChange,
    placeholder = placeholder,
    singleLine = true,
    colors = TextFieldDefaults.textFieldColors(
        containerColor = MaterialTheme.colorScheme.background,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary
    )
)

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
            CustomTextField(Modifier.weight(3f), text, {if (it.length < 46) text = it}, {Text("Endpoint IP Address...")})
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
