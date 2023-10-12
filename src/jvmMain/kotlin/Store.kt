import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class UiState(val text: String = "")

class ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState>
        get() = _uiState

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
