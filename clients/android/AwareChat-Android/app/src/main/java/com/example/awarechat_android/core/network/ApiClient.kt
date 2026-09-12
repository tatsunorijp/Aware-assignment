package com.example.awarechat_android.core.network

import com.example.awarechat_android.core.protocol.HealthResponse
import com.example.awarechat_android.core.protocol.UserDto
import com.example.awarechat_android.core.protocol.UsersResponse

interface ApiClientContract {
    suspend fun health(): HealthResponse
    suspend fun users(): List<UserDto>
}

class ApiClient(
    private val configuration: NetworkConfiguration,
    private val networkClient: NetworkClient = NetworkManager(),
) : ApiClientContract {
    override suspend fun health(): HealthResponse = networkClient.fetch(
        request = ApiEndpoint.Health.request(configuration),
        deserializer = HealthResponse.serializer(),
    )

    override suspend fun users(): List<UserDto> = networkClient.fetch(
        request = ApiEndpoint.Users.request(configuration),
        deserializer = UsersResponse.serializer(),
    ).users
}
