package ru.grabovsky.dungeoncrusherbot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.grabovsky.dungeoncrusherbot.entity.Server

@Repository
interface ServerRepository: JpaRepository<Server, Int> {
    fun findServerById(id: Int): Server?
}