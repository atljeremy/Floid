package com.jeremyfox.floid.data

data class Output<out Action, out Data>(
        val action: Action,
        val data: Data?
)