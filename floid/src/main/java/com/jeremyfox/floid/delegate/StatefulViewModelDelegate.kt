package com.jeremyfox.floid.delegate

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ExperimentalCoroutinesApi
interface StatefulViewModelDelegate<Action, Data, State> {
    val state: MutableLiveData<State>
    val actions: StateFlow<Action?>
    fun dispatch(action: Action)
}

@ExperimentalCoroutinesApi
class StatefulViewModelImpl<Action, Data, State> : StatefulViewModelDelegate<Action, Data, State> {
    override val state = MutableLiveData<State>()

    private val _actions = MutableStateFlow<Action?>(null)
    override val actions: StateFlow<Action?>
        get() = _actions

    override fun dispatch(action: Action) {
        _actions.value = action
    }
}