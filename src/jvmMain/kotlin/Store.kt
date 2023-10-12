import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.net.Socket;
import java.net.InetAddress

data class UiState(val text: String = "")

private const val PORT = 5000
private const val IP = ""

class ViewModel {
    private val _uiState = MutableStateFlow(UiState())
    private val _socket = IP//Socket(IP, PORT);
    init {
        println(_socket)
    }
    val uiState: StateFlow<UiState>
        get() = _uiState

    val socket: String
        get() = _socket

    fun updateUiState(state: UiState) {
        _uiState.update {
            state
        }
    }

    companion object {
        private var viewModel: ViewModel? = null

        fun Factory(): ViewModel {
             if (viewModel == null) {
                viewModel = ViewModel()
             }
            return viewModel as ViewModel
        }
    }
}
