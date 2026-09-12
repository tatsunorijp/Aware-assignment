package com.example.awarechat_android.core.network

import com.example.awarechat_android.core.protocol.HttpErrorEnvelope
import com.example.awarechat_android.core.protocol.ProtocolCodec
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Request

interface NetworkClient {
    suspend fun <Response> fetch(
        request: Request,
        deserializer: DeserializationStrategy<Response>,
    ): Response
}

class NetworkManager(
    private val transport: HttpTransport = OkHttpTransport(),
    private val json: Json = ProtocolCodec.defaultJson,
) : NetworkClient {
    override suspend fun <Response> fetch(
        request: Request,
        deserializer: DeserializationStrategy<Response>,
    ): Response {
        val response = try {
            transport.execute(request)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            throw NetworkException.Transport(error)
        } catch (error: NetworkException) {
            throw error
        } catch (error: Throwable) {
            throw NetworkException.TransportFailure()
        }

        if (response.statusCode !in 200..299) {
            val envelope = try {
                json.decodeFromString(HttpErrorEnvelope.serializer(), response.body)
            } catch (_: Throwable) {
                throw NetworkException.InvalidErrorResponse(response.statusCode)
            }
            throw NetworkException.Server(response.statusCode, envelope.error)
        }

        return try {
            json.decodeFromString(deserializer, response.body)
        } catch (_: SerializationException) {
            throw NetworkException.Decoding()
        } catch (_: IllegalArgumentException) {
            throw NetworkException.Decoding()
        }
    }
}
