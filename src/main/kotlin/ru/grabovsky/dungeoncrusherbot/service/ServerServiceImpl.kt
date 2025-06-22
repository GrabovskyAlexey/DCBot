package ru.grabovsky.dungeoncrusherbot.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import ru.grabovsky.dungeoncrusherbot.entity.Server
import ru.grabovsky.dungeoncrusherbot.repository.ServerRepository
import ru.grabovsky.dungeoncrusherbot.service.interfaces.ServerService

@Service
class ServerServiceImpl(
    private val serverRepository: ServerRepository
) : ServerService {
    override fun getServerById(serverId: Int)=
        serverRepository.findServerById(serverId) ?: throw EntityNotFoundException("Not found server with id: $serverId")

    override fun getAllServers(): List<Server> =  serverRepository.findAll()

}