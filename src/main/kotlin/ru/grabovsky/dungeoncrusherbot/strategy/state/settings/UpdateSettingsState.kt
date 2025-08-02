package ru.grabovsky.dungeoncrusherbot.strategy.state.settings

import org.springframework.stereotype.Component
import ru.grabovsky.dungeoncrusherbot.service.interfaces.StateService

@Component
class UpdateSettingsState(stateService: StateService) : SettingsState(stateService)