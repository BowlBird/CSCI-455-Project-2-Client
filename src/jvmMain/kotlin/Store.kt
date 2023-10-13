import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.net.InetSocketAddress
import java.net.Socket;
import java.net.SocketAddress
import kotlin.concurrent.thread

data class UiState(val text: String = "", val messages: List<String> = listOf())

private const val PORT = 50000

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
        thread {
            while(!connectionRepository.isConnected) {
                connectionRepository.openConnection("", PORT)
                Thread.sleep(1000)
            }
        }
    }

    private val _uiState = MutableStateFlow(UiState())

    val uiState: StateFlow<UiState>
        get() = _uiState

    fun openConnection(ip: String = "") = connectionRepository.openConnection(ip, PORT)

    fun closeConnection() = connectionRepository.closeConnection()

    fun updateUiState(state: UiState) {
        _uiState.update {
            state
        }
    }
}
