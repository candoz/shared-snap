package service

import io.netty.handler.codec.http.HttpResponseStatus.*
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import java.lang.Exception
import java.util.*

object RequestManager {
    private var vertx: Vertx? = null
    private val CONFIG = json {
        obj(
            "connection_string" to "mongodb://localhost:27017/snapdb")
    }
    private const val CONNECTIONS_COLLECTION = "connections"
    private const val DOCUMENT_ID = "_id"
    private const val CONNECTION_ID = "nickname"
    private const val TOKEN = "token"
    private const val AUTHORIZATION = "Authorization"
    private const val DUPLICATED_KEY_CODE = "E11000"

    fun initializeRequestManager(vertx: Vertx) {
        RequestManager.vertx = vertx
    }

    fun createConnection(context: RoutingContext) {
        val response = context.response()
        val nickname = context.request().getParam(CONNECTION_ID)
        val token = UUID.randomUUID().toString()
        val document = json {
            obj(
                DOCUMENT_ID to nickname,
                TOKEN to token)
        }
        MongoClient.createNonShared(vertx, CONFIG).insert(CONNECTIONS_COLLECTION, document) { result ->
            when {
                result.succeeded() -> response
                    .putHeader("Content-Type", "text/plain")
                    .setStatusCode(CREATED.code())
                    .end(Json.encodePrettily(token))
                isDuplicateKey(result.cause().message) -> response.setStatusCode(CONFLICT.code()).end()
                else -> response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
            }
        }
    }

    private fun isDuplicateKey(errorMessage: String?) = errorMessage?.startsWith(DUPLICATED_KEY_CODE) ?: false

    fun deleteConnection(context: RoutingContext) {
        val response = context.response()
        val nickname = context.request().getParam(CONNECTION_ID)
        try {
            val token = context.request().getHeader(AUTHORIZATION)
            UUID.fromString(token)
            val document = json {
                obj(
                    DOCUMENT_ID to nickname,
                    TOKEN to token)
            }
            MongoClient.createNonShared(vertx, CONFIG).findOneAndDelete(CONNECTIONS_COLLECTION, document) { result ->
                when {
                    result.succeeded() ->
                        result.result()?.let { response.setStatusCode(NO_CONTENT.code()).end() }
                            ?: response.setStatusCode(NOT_FOUND.code()).end()
                    else -> response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                }
            }
        } catch (ex: Exception) {
            response.setStatusCode(BAD_REQUEST.code()).end()
        }
    }

    fun createMessage(context: RoutingContext) {
    }

    fun retrieveMessages(context: RoutingContext) {
    }

    fun deleteMessages(context: RoutingContext) {
    }

    fun deleteSingleMessage(context: RoutingContext) {
    }

    fun retrieveOldestMessage(context: RoutingContext) {
    }

    fun retrieveLatestMessage(context: RoutingContext) {
    }
}
