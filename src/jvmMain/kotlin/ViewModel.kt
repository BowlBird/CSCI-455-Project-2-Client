import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.lang.IllegalArgumentException
import kotlin.concurrent.thread

/**
 * Immutable Ui State Holder,
 * this gives the Ui information about what should be presented.
 * All default values are empty.
 */
data class UiState(
    val connection: String = "",
    val currentFundraisers: List<Fundraiser> = listOf(),
    val oldFundraisers: List<Fundraiser> = listOf()
)

/**
 * Super class for all Response Types, generated the hashmap that
 * stores the keys that come in from the response from the server
 * for easy parsing.
 */
sealed class Response(response: String) {
    protected var map = HashMap<String, String>()
    init {
        //new line delimited
        response.split("\n").forEach {
            //... : ... syntax
            val (key, value) = it.split(" : ")
            map[key] = value
        }
    }
}

/**
 * Represents a response to the Clients Create Fundraiser Request.
 * Only an ID is returned, which is the instance variable.
 */
class CreateResponse(response: String) : Response(response) {
    var id: Int = 0
        private set

    init {
        //initializes the ID
        id = map["id"]?.toInt() ?: throw IllegalArgumentException("No ID!")
    }
}

/**
 * Represents a response to the Clients List Request
 * this stores a list of fundraisers and it's information so the UI
 * can show it.
 *
 * The UI is separated between current and old Fundraisers, which is what
 * the current boolean represents.
 */
class ListResponse(response: String) : Response(response) {
    var items: List<Fundraiser> = listOf()
        private set

    var current: Boolean = false
        private set

    init {
        current = map["current"].toBoolean()
        //since we only want to deal with the actual fundraiser instances
        //which have integer keys, we can check if the key in the map
        //is able to be parsed to see if it's what we want.
        map.keys.filter { it.toIntOrNull() != null }.forEach {
            val key = it.toInt()
            val value = map[it]
            val (name, goal, balance, deadline) = value?.split(",") ?: throw Exception("Element doesn't exist!")
            items += Fundraiser(key,name, goal.toDouble(),balance.toDouble(),  deadline)
        }
    }
}

/**
 * An immutable class representing a single fundraiser and all it's information.
 */
data class Fundraiser(val id: Int, val name: String, val goal: Double, val balance: Double, val deadline: String)

/**
 * Represents a response to our Donation request. There is no data to store, so this
 * is purely to generate events based on its existence.
 */
class DonateResponse(response: String) : Response(response)

/**
 * Represents a response to our Balance request. Holds the amount of the Fundraiser
 * who's ID we gave in the Clients Request.
 */
class BalanceResponse(response: String) : Response(response) {
    var amount: Double = 0.0
        private set

    init {
        amount = map["amount"]?.toDouble() ?: throw IllegalArgumentException("No amount!")
    }
}

// default values for when no other ip is given
private const val PORT = 50000
private const val LOOPBACK_IP = "127.0.0.1"

/**
 * Controller for the UI and State in general. Allows for interacting with the server
 * and database through the repository.
 */
class ViewModel private constructor(private val connectionRepository: ConnectionRepository<String>) {
    companion object {
        //holds reference to enforce singleton pattern
        @Volatile
        private var _viewModel: ViewModel? = null
        //Factory method for either getting the reference or generating a new ViewModel.
        val Factory
            get() = _viewModel ?: synchronized(this) {
                ViewModel(ConnectionRepository()).also { _viewModel = it }
            }
    }
    init {
        //initializing onReceive callback method
        connectionRepository.onReceive = {
            //there may be more than one message received between callbacks, so iterate through them all.
            repeat(connectionRepository.messages.size) {
                val message = connectionRepository.popMessage()
                //create subclass depending on message type
                val response: Response? = when(message?.split("\n")?.get(0)?.split(" : ")?.get(1)) {
                    "CREATE" -> CreateResponse(message)
                    "LIST" -> ListResponse(message)
                    "DONATE" -> DonateResponse(message)
                    "BALANCE" -> BalanceResponse(message)
                    else -> null
                }
                //should never be null, so check is necessary.
                if (response != null) {
                    when(response) {
                        is ListResponse -> {
                            //extract if this should update current or old fundraisers
                            val currentFundraisers = if (response.current) response.items.sortedBy { it.deadline } else _uiState.value.currentFundraisers
                            val oldFundraisers = if (!response.current) response.items.sortedBy { it.deadline } else _uiState.value.currentFundraisers
                            //update UI
                            updateUiState(
                                _uiState.value.copy(
                                    currentFundraisers = currentFundraisers,
                                    oldFundraisers = oldFundraisers
                                )
                            )
                        }
                        //re-acquire lists on update.
                        is CreateResponse -> {
                            sendMessage("endpoint : LIST\ncurrent : true")
                            sendMessage("endpoint : LIST\ncurrent : false")
                        }
                        is BalanceResponse -> {}
                        is DonateResponse -> {
                            sendMessage("endpoint : LIST\ncurrent : true")
                            sendMessage("endpoint : LIST\ncurrent : false")
                        }
                    }
                }
            }
        }
        //setup onEndpointDisconnect callback
        connectionRepository.onEndpointDisconnect = {
            //update UI to say no connection
            updateUiState(
                uiState.value.copy(
                    connection = connectionRepository.connection ?: ""
                )
            )
            //try to reconnect to loopback connection
            openLoopbackConnection()
        }
        //setup onConnect callback
        connectionRepository.onConnect = {
            //update UI
            updateUiState(
                uiState.value.copy(
                    connection = connectionRepository.connection ?: ""
                )
            )
            //request preliminary data
            sendMessage("endpoint : LIST\ncurrent : true")
            sendMessage("endpoint : LIST\ncurrent : false")
        }
        //search loopback connection
        openLoopbackConnection()
    }

    private val _uiState = MutableStateFlow(UiState())

    private fun openLoopbackConnection() =
        //start new thread to not lock up UI Thread
        thread {
            //check if there is a connection already, if so quit.
            while(!connectionRepository.isConnected) {
                println("No Connection Present. Trying to use Loopback IP.")
                connectionRepository.openConnection(LOOPBACK_IP, PORT)
                Thread.sleep(5000) //recheck every 5 seconds
            }
        }


    val uiState: StateFlow<UiState>
        get() = _uiState

    /**
     * Tell the ConnectionRepository to open a connection to the ip given
     * or loopback IP if no ip is given.
     */
    fun openConnection(ip: String = LOOPBACK_IP) {
        println("Requesting to open a connection with $ip on Port $PORT")
        val result = connectionRepository.openConnection(ip, PORT)
    }

    /**
     * Tell the ConnectionRepository to close the current connection.
     */
    fun closeConnection() {
        println("Connection Interrupt Requested.")
        connectionRepository.closeConnection()
        openLoopbackConnection()
    }

    /**
     * Updates UI when passed a new state.
     */
    fun updateUiState(state: UiState) {
        _uiState.update {
            state
        }
    }

    /**
     * sends a message that the UI built to the connection
     */
    fun sendMessage(message: String): Boolean {
        println("Trying to Send '${message.replace("\n", "\\n")}' to '${connectionRepository.connection}'")
        return connectionRepository.sendMessage(message)
    }

}
