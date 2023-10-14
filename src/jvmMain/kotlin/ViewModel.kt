import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.thread

data class UiState(
    val connection: String = "",
    val messages: List<String> = listOf()
)

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
            updateUiState(
                uiState.value.copy(
                    messages = connectionRepository.messages
                )
            )
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
        println("Trying to Send '$message' to '${connectionRepository.connection}'")
        return connectionRepository.sendMessage(message)
    }

}
