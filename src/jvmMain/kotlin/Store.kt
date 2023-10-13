import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.net.InetSocketAddress
import java.net.Socket;
import java.net.SocketAddress

data class UiState(val text: String = "")

private const val PORT = 50000

class ViewModel private constructor() {
    companion object {
        @Volatile
        private var _viewModel: ViewModel? = null
        val Factory
            get() = _viewModel ?: synchronized(this) { ViewModel().also { _viewModel = it } }
    }

    private val _uiState = MutableStateFlow(UiState())

    val uiState: StateFlow<UiState>
        get() = _uiState

    var connection: Connection<String>? = null
        private set

    fun openConnection(ip: String = ""): Connection<String>? {
        val socket = try { Socket(ip, PORT) } catch (_: Exception) {null} ?: return null
        connection?.interrupt()
        connection = Connection(socket)
        return connection
    }

    fun closeConnection() {
        connection = null
    }

    fun updateUiState(state: UiState) {
        _uiState.update {
            state
        }
    }
}
