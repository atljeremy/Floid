package com.jeremyfox.floid.reducer

import com.jeremyfox.floid.data.Output
import kotlinx.coroutines.flow.Flow

interface Reducer<Action, Data, State> {
    val initialState: State
    suspend fun perform(action: Action): Flow<Output<Action, Data>>
    suspend fun reduce(action: Action, state: State, data: Data?): State
}