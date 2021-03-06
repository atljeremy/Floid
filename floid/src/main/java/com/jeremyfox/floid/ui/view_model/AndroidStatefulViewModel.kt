package com.jeremyfox.floid.ui.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jeremyfox.floid.delegate.StatefulViewModelDelegate
import com.jeremyfox.floid.delegate.StatefulViewModelImpl
import com.jeremyfox.floid.reducer.Reducer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
abstract class AndroidStatefulViewModel<Action, Data, State>(
        application: Application,
        delegate: StatefulViewModelDelegate<Action, Data, State> = StatefulViewModelImpl()
) : AndroidViewModel(application), StatefulViewModelDelegate<Action, Data, State> by delegate {

    private val _state: State
        get() = state.value ?: this.reducer().initialState

    init {
        bindActions()
    }

    abstract fun reducer(): Reducer<Action, Data, State>
    abstract fun bind(flow: Flow<State>): Flow<State>

    private fun bindActions() {
        val flow = actions.filterNotNull()
                .flatMapConcat(this.reducer()::perform)
                .map { this.reducer().reduce(it.action, _state, it.data) }

        bind(flow)
                .onEach(state::setValue)
                .launchIn(viewModelScope)
    }

}