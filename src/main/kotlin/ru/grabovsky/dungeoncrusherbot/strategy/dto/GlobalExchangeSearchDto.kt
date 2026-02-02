package ru.grabovsky.dungeoncrusherbot.strategy.dto

data class GlobalExchangeSearchDto(
    val username: String?,
    val matchGroups: List<MatchGroup>
) {
    data class MatchGroup(
        val userRequest: ExchangeRequestDto,
        val matches: List<MatchDto>
    )

    data class MatchDto(
        val request: ExchangeRequestDto,
        val ownerUsername: String?,
        val ownerFirstName: String
    )

    val hasMatches: Boolean get() = matchGroups.isNotEmpty()
    val totalMatchesCount: Int get() = matchGroups.sumOf { it.matches.size }
}
