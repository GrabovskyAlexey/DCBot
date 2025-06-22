package ru.grabovsky.dungeoncrusherbot.strategy.data

import org.springframework.stereotype.Repository
import org.telegram.telegrambots.meta.api.objects.User
import ru.grabovsky.dungeoncrusherbot.strategy.dto.DataModel
import ru.grabovsky.dungeoncrusherbot.strategy.state.StateCode
import ru.grabovsky.dungeoncrusherbot.util.CommonUtils.currentStateCode

@Repository
abstract class AbstractDataRepository<T: DataModel> {
    abstract fun getData(user: User): T

    fun isAvailableForCurrentState(stateCode: StateCode)=
        this.currentStateCode("DataRepository") == stateCode


}