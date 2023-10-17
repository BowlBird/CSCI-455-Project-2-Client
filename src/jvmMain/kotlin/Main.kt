import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.time.LocalDate
import kotlin.math.round

/**
 * Root App Composable
 */
@Composable
fun App() = AppTheme(true) {
    //renders the background
    Surface(Modifier.fillMaxSize(), color = Color(red = 11, blue = 11, green = 11)) {

        //get ViewModel reference
        val viewModel = ViewModel.Factory
        val state = viewModel.uiState.collectAsState().value

        Column {
            ConnectionBar() //top bar
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    //holds references to user input for creating a new fundraiser
                    var name by remember { mutableStateOf("") }
                    var amount by remember { mutableStateOf("") }
                    var deadline by remember { mutableStateOf("") }

                    Button(modifier = Modifier.fillMaxWidth().padding(5.dp), onClick = {
                        try {
                            //test if the date passed is acceptable
                            LocalDate.parse(deadline)
                            //tell endpoint to create the fundraiser
                            viewModel.sendMessage(
                                "endpoint : CREATE\n" +
                                "name : $name\n" +
                                "targetAmount : ${amount.toDouble()}\n" +
                                "deadline : $deadline"
                            )
                            // reset user input
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
                        //user input fields
                        CustomTextField(Modifier.weight(1f), name, { name = it }, { Text("Funraiser Name...") })
                        CustomTextField(Modifier.weight(1f), amount, { amount = it }, { Text("Target Amount...") })
                        CustomTextField(
                            Modifier.weight(1f),
                            deadline,
                            { deadline = it },
                            { Text("Deadline (YYYY-MM-DD)...") })
                    }
                }
                //holds lists
                Row(Modifier.fillMaxSize()) {
                    //lazy column to allow for scrolling
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(5.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.background),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Text("Current Fundraisers", color = MaterialTheme.colorScheme.onBackground)
                        }
                        repeat(state.currentFundraisers.size) {
                            //render list items
                            item {
                                val fundraiser = state.currentFundraisers[it]
                                Row(modifier = Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        modifier = Modifier.padding(5.dp),
                                        text = (it + 1).toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    //pass fundraiser to composable
                                    Fundraiser(fundraiser = fundraiser)
                                }
                            }
                        }
                    }
                    //lazy column for allowing for scrolling
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(5.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.background),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Text("Old Fundraisers", color = MaterialTheme.colorScheme.onBackground)
                        }
                        //render old fundraisers
                        repeat(state.oldFundraisers.size) {
                            item {
                                val fundraiser = state.oldFundraisers[it]
                                Row(modifier = Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        modifier = Modifier.padding(5.dp),
                                        text = (it + 1).toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    //pass fundraiser reference to composable
                                    Fundraiser(fundraiser = fundraiser)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Logic for rendering fundraisers on screen
 */
@Composable
fun Fundraiser(modifier: Modifier = Modifier, fundraiser: Fundraiser) {
    //handles whether the ui should render the donation part
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
        //renders the percent completion
        LinearProgressIndicator((fundraiser.balance/fundraiser.goal).toFloat(), Modifier
            .fillMaxWidth()
            .padding(5.dp, 0.dp)
            .clip(
                RoundedCornerShape(5.dp)
            ),
            MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.background)
        Text("$${String.format("%,.2f", fundraiser.balance)} raised out of $${String.format("%,.2f", fundraiser.goal)}")
        Text(fundraiser.deadline)
        //allows for animation when opening the composable
        AnimatedVisibility(opened) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                var amount by remember { mutableStateOf("") }
                CustomTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = amount,
                    onValueChange = {
                            amount = it
                    },
                    placeholder = { Text("Amount To Donate...") })
                Button(modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                    shape = RoundedCornerShape(5.dp),
                    onClick = {

                    val viewModel = ViewModel.Factory
                    //see if the donation textfield can be parsed as money
                    if (amount.toDoubleOrNull() != null) {
                        //see if the amount is at least one cent
                        if (round(amount.toDouble() * 100) / 100 > 0) {
                            val doubleAmount = amount.toDouble()
                            //tell the endpoint to donate the amount given
                            viewModel.sendMessage(
                                "endpoint : DONATE\nid : ${fundraiser.id}\namount : $doubleAmount"
                            )
                        }
                    }
                }) {
                    Text("Donate!")
                }
            }
        }
    }
}

/**
 * reuseable Composable to allow for consistency between textfields
 */
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

/**
 * Draws the top bar
 */
@Composable
fun ConnectionBar(modifier: Modifier = Modifier) = NavigationBar(modifier) {
    //get viewmodel references
    val viewModel = ViewModel.Factory
    val state = viewModel.uiState.collectAsState().value

    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(2f).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            //display who the client is connected to
            Text(
                modifier = Modifier.padding(5.dp),
                text = if (state.connection != "") "Connected To: ${state.connection}" else "Not Connected...",
                maxLines = 1
            )
            Row {
                //allow for disconnecting
                Button(
                    modifier = Modifier.padding(5.dp).fillMaxSize().clip(RoundedCornerShape(5.dp)).weight(1f),
                    onClick = {
                        viewModel.closeConnection()
                    },
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text("Disconnect")
                }
                //allow for refreshing data
                Button(
                    modifier = Modifier.padding(5.dp).fillMaxSize().clip(RoundedCornerShape(5.dp)).weight(1f),
                    onClick = {
                        viewModel.sendMessage("endpoint : LIST\ncurrent : true")
                        viewModel.sendMessage("endpoint : LIST\ncurrent : false")
                    },
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text("Refresh")
                }
            }
        }

        Row(Modifier.padding(5.dp).weight(4f), verticalAlignment = Alignment.CenterVertically) {
            var text by remember { mutableStateOf("") }
            CustomTextField(Modifier.weight(3f), text, {if (it.length < 46) text = it}, {Text("Endpoint IP Address...")})
            Button(modifier = Modifier.padding(5.dp).fillMaxHeight(), onClick = {
                viewModel.openConnection(text) //parse and try to connect
                },
                shape = RoundedCornerShape(5.dp)
            ) {
                Text("Connect")
            }

        }
    }
}

/**
 * Main application start point
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GoFundMe Client",
        state = WindowState(size = DpSize(1400.dp,1000.dp))
    ) {
        App()
    }
}
