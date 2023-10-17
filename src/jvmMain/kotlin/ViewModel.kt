import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.lang.IllegalArgumentException
import kotlin.concurrent.thread

data class UiState(
    val connection: String = "",
    val currentFundraisers: List<Fundraiser> = listOf(),
    val oldFundraisers: List<Fundraiser> = listOf()
)


sealed class Response(response: String) {
    protected var map = HashMap<String, String>()
    init {
        response.split("\n").forEach {
            val (key, value) = it.split(" : ")
            map[key] = value
        }
    }
}
class CreateResponse(response: String) : Response(response) {
    var id: Int = 0
        private set

    init {
        id = map["id"]?.toInt() ?: throw IllegalArgumentException("No ID!")
    }
}
class ListResponse(response: String) : Response(response) {
    var items: List<Fundraiser> = listOf()
        private set

    var current: Boolean = false
        private set

    init {
        current = map["current"].toBoolean()
        map.keys.filter { it.toIntOrNull() != null }.forEach {
            val key = it.toInt()
            val value = map[it]
            val (name, goal, balance, deadline) = value?.split(",") ?: throw Exception("Element doesn't exist!")
            items += Fundraiser(key,name, goal.toDouble(),balance.toDouble(),  deadline)
        }
    }
}
data class Fundraiser(val id: Int, val name: String, val goal: Double, val balance: Double, val deadline: String)
class DonateResponse(response: String) : Response(response)
class BalanceResponse(response: String) : Response(response) {
    var amount: Double = 0.0
        private set

    init {
        amount = map["amount"]?.toDouble() ?: throw IllegalArgumentException("No amount!")
    }
}

private const val PORT = 50000
private const val LOOPBACK_IP = "127.0.0.1"

class ViewModel private constructor(private val connectionRepository: ConnectionRepository<String>) {
    companion object {
        @Volatile
        private var _viewModel: ViewModel? = null
        val Factory
            get() = _viewModel ?: synchronized(this) {
                ViewModel(ConnectionRepository()).also { _viewModel = it }
            }
    }
    init {
        connectionRepository.onReceive = {
            repeat(connectionRepository.messages.size) {
                val message = connectionRepository.popMessage()
                val response: Response? = when(message?.split("\n")?.get(0)?.split(" : ")?.get(1)) {
                    "CREATE" -> CreateResponse(message)
                    "LIST" -> ListResponse(message)
                    "DONATE" -> DonateResponse(message)
                    "BALANCE" -> BalanceResponse(message)
                    else -> null
                }
                if (response != null) {
                    when(response) {
                        is ListResponse -> {
                            val currentFundraisers = if (response.current) response.items.sortedBy { it.deadline } else _uiState.value.currentFundraisers
                            val oldFundraisers = if (!response.current) response.items.sortedBy { it.deadline } else _uiState.value.currentFundraisers
                            println("CURRENT: $currentFundraisers")
                            println("OLD: $oldFundraisers")
                            updateUiState(
                                _uiState.value.copy(
                                    currentFundraisers = currentFundraisers,
                                    oldFundraisers = oldFundraisers
                                )
                            )
                        }
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
        connectionRepository.onEndpointDisconnect = {
            updateUiState(
                uiState.value.copy(
                    connection = connectionRepository.connection ?: ""
                )
            )
            openLoopbackConnection()
        }
        connectionRepository.onConnect = {
            updateUiState(
                uiState.value.copy(
                    connection = connectionRepository.connection ?: ""
                )
            )
            //request preliminary data
            sendMessage("endpoint : LIST\ncurrent : true")
            sendMessage("endpoint : LIST\ncurrent : false")
        }
        openLoopbackConnection()
    }

    private val _uiState = MutableStateFlow(UiState())

    private fun openLoopbackConnection() =
        thread {
            while(!connectionRepository.isConnected) {
                println("No Connection Present. Trying to use Loopback IP.")
                connectionRepository.openConnection(LOOPBACK_IP, PORT)
                Thread.sleep(5000)
            }
        }


    val uiState: StateFlow<UiState>
        get() = _uiState

    fun openConnection(ip: String = LOOPBACK_IP) {
        println("Requesting to open a connection with $ip on Port $PORT")
        val result = connectionRepository.openConnection(ip, PORT)
    }

    fun closeConnection() {
        println("Connection Interrupt Requested.")
        connectionRepository.closeConnection()
        openLoopbackConnection()
    }

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
