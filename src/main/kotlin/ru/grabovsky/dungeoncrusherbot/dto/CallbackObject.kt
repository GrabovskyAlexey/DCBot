package ru.grabovsky.dungeoncrusherbot.dto

import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode

data class CallbackObject (val state: StateCode, val data: String)