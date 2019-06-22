package service

import io.netty.handler.codec.http.HttpResponseStatus.CONFLICT
import io.netty.handler.codec.http.HttpResponseStatus.CREATED
import io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR
import io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND
import io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.OK
import java.lang.Exception
import java.util.UUID
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.FindOptions
import io.vertx.kotlin.core.json.get
import java.time.Instant

object RequestManager {
    private var vertx: Vertx? = null
    private val MONGO_CONFIG = json { obj(
        "connection_string" to "mongodb://localhost:27017/snapdb"
    ) }
    private const val CONNECTIONS_COLLECTION = "connections"
    private const val MESSAGES_COLLECTION = "messages"
    private const val DEFAULT_ID = "_id"
    private const val NICKNAME = "nickname"
    private const val TOKEN = "token"
    private const val SENDER = "sender"
    private const val RECIPIENT = "recipient"
    private const val DATE_TIME = "datetime"
    private const val CONTENT = "content"
    private const val MESSAGE_ID = "messageId"
    private const val AUTHORIZATION = "Authorization"
    private const val DUPLICATED_KEY_CODE = "E11000"

    private fun isDuplicateKey(errorMessage: String?) = errorMessage?.startsWith(DUPLICATED_KEY_CODE) ?: false

    private fun dropAllCollections() {
        val client = MongoClient.createNonShared(vertx, MONGO_CONFIG)
        client.getCollections { res ->
            if (res.succeeded()) {
                val collections = res.result()
                for (c in collections) {
                    client.dropCollection(c) { }
                }
            }
        }
    }

    fun initializeRequestManager(vertx: Vertx) {
        RequestManager.vertx = vertx
        dropAllCollections()
    }

    fun createConnection(context: RoutingContext) {
        val response = context.response().putHeader("Access-Control-Allow-Origin", "*")
            .putHeader("Access-Control-Allow-Headers", "*")
        val nickname = context.request().getParam(NICKNAME)
        val token = UUID.randomUUID().toString()
        val document = json { obj(
            DEFAULT_ID to nickname,
            TOKEN to token
        ) }
        MongoClient.createNonShared(vertx, MONGO_CONFIG).insert(CONNECTIONS_COLLECTION, document) { insertOperation ->
            when {
                insertOperation.succeeded() -> response
                    .putHeader("Content-Type", "text/plain")
                    .setStatusCode(CREATED.code())
                    .end(token)
                isDuplicateKey(insertOperation.cause().message) -> response.setStatusCode(CONFLICT.code()).end()
                else -> response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
            }
        }
    }

    fun deleteConnection(context: RoutingContext) {
        val response = context.response().putHeader("Access-Control-Allow-Origin", "*")
        val nickname = context.request().getParam(NICKNAME)
        try {
            val token = context.request().getHeader(AUTHORIZATION)
            UUID.fromString(token)
            val document = json { obj(
                DEFAULT_ID to nickname,
                TOKEN to token
            ) }
            MongoClient.createNonShared(vertx, MONGO_CONFIG).removeDocument(CONNECTIONS_COLLECTION, document) { removeOperation ->
                when {
                    removeOperation.succeeded() ->
                        if (removeOperation.result().removedCount == 0L)
                            response.setStatusCode(NOT_FOUND.code()).end()
                        else
                            response.setStatusCode(NO_CONTENT.code()).end()
                    else -> response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                }
            }
        } catch (ex: Exception) {
            response.setStatusCode(BAD_REQUEST.code()).end()
        }
    }

    fun optionsMessage(context: RoutingContext) {
        println("Inside OPTIONS")
        context.response()
            .putHeader("Access-Control-Allow-Origin", "*")
            .putHeader("Access-Control-Allow-Headers", "*")
            .putHeader("Access-Control-Allow-Methods", "*")
            .setStatusCode(OK.code())
            .end()
    }
    fun createMessage(context: RoutingContext) {
        println("Inside POST")
        val response = context.response()
            .putHeader("Access-Control-Allow-Origin", "*")
            .putHeader("Access-Control-Allow-Headers", "*")
            .putHeader("Access-Control-Allow-Methods", "*")
        val recipient = context.request().getParam(NICKNAME)
        val token = context.request().getHeader(AUTHORIZATION)
        val body = context.bodyAsJson

        /* First of all, check authentication token: */
        val queryAuth = json { obj(
            TOKEN to token
        ) }
        MongoClient.createNonShared(vertx, MONGO_CONFIG).find(CONNECTIONS_COLLECTION, queryAuth) { findOperation ->
            when {
                findOperation.succeeded() -> {
                    val results: List<JsonObject> = findOperation.result()
                    if (results.isEmpty()) {
                        response.setStatusCode(BAD_REQUEST.code()).end()
                    } else { /* Authentication OK, check if recipient exists: */
                        val queryNickname = json { obj(
                            DEFAULT_ID to recipient
                        ) }

                        MongoClient.createNonShared(vertx, MONGO_CONFIG).find(CONNECTIONS_COLLECTION, queryNickname) { findOperation2 ->
                            when {
                                findOperation2.succeeded() -> {
                                    val results2: List<JsonObject> = findOperation2.result()
                                    if (results2.isEmpty()) {
                                        response.setStatusCode(NOT_FOUND.code()).end()
                                    } else { /* Recipient exists, add the new message: */
                                        val messageId = UUID.randomUUID().toString()
                                        val newMessageDocument = json { obj(
                                            DEFAULT_ID to messageId,
                                            SENDER to results[0][DEFAULT_ID],
                                            RECIPIENT to recipient,
                                            DATE_TIME to Instant.now(),
                                            CONTENT to body
                                        ) }
                                        MongoClient.createNonShared(vertx, MONGO_CONFIG).insert(MESSAGES_COLLECTION, newMessageDocument) { createOperation ->
                                            when {
                                                createOperation.succeeded() -> response.setStatusCode(CREATED.code()).end()
                                                isDuplicateKey(createOperation.cause().message) -> createMessage(context)
                                                else -> response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                                            }
                                        }
                                    }
                                }
                                findOperation2.failed() -> {
                                    response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                                }
                            }
                        }
                    }
                }
                findOperation.failed() -> {
                    response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                }
            }
        }
    }

    fun retrieveMessages(context: RoutingContext) {
        val response = context.response().putHeader("Access-Control-Allow-Origin", "*")
        val nickname = context.request().getParam(NICKNAME)
        val token = context.request().getHeader(AUTHORIZATION)

        /* First of all, check authentication token: */
        val queryAuth = json { obj(
            DEFAULT_ID to nickname,
            TOKEN to token
        ) }
        MongoClient.createNonShared(vertx, MONGO_CONFIG).find(CONNECTIONS_COLLECTION, queryAuth) { findOperation ->
            when {
                findOperation.succeeded() -> {
                    val results: List<JsonObject> = findOperation.result()
                    if (results.isEmpty()) {
                        response.setStatusCode(BAD_REQUEST.code()).end()
                    } else { /* Authentication OK, check for my messages */
                        val queryMessages = json { obj(
                            RECIPIENT to nickname
                        ) }

                        MongoClient.createNonShared(vertx, MONGO_CONFIG).find(MESSAGES_COLLECTION, queryMessages) { findOperation2 ->
                            when {
                                findOperation2.succeeded() -> {
                                    val results2: List<JsonObject> = findOperation2.result()
                                    if (results2.isEmpty()) {
                                        response.setStatusCode(NO_CONTENT.code()).end()
                                    } else { /* There is at least one message for me */

                                        val responseBody = results2.map { msg -> json { obj(
                                            "id" to msg[DEFAULT_ID],
                                            "sender" to msg[SENDER],
                                            "content" to msg[CONTENT]
                                        ) } }
                                        response.setStatusCode(OK.code()).end(Json.encodePrettily(responseBody))
                                    }
                                }
                                findOperation2.failed() -> {
                                    response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                                }
                            }
                        }
                    }
                }
                findOperation.failed() -> {
                    response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                }
            }
        }
    }

    fun deleteMessages(context: RoutingContext) {
        val response = context.response().putHeader("Access-Control-Allow-Origin", "*")
        val nickname = context.request().getParam(NICKNAME)
        val token = context.request().getHeader(AUTHORIZATION)

        /* First of all, check authentication token: */
        val queryAuth = json { obj(
            DEFAULT_ID to nickname,
            TOKEN to token
        ) }
        MongoClient.createNonShared(vertx, MONGO_CONFIG).find(CONNECTIONS_COLLECTION, queryAuth) { findOperation ->
            when {
                findOperation.succeeded() -> {
                    val results: List<JsonObject> = findOperation.result()
                    if (results.isEmpty()) {
                        response.setStatusCode(BAD_REQUEST.code()).end()
                    } else { /* Authentication OK, check for my messages */
                        val queryMessages = json { obj(
                            RECIPIENT to nickname
                        ) }

                        MongoClient.createNonShared(vertx, MONGO_CONFIG).removeDocuments(MESSAGES_COLLECTION, queryMessages) { findOperation2 ->
                            when {
                                findOperation2.succeeded() -> {
                                    response.setStatusCode(NO_CONTENT.code()).end()
                                }
                                findOperation2.failed() -> {
                                    response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                                }
                            }
                        }
                    }
                }
                findOperation.failed() -> {
                    response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                }
            }
        }
    }

    fun deleteSingleMessage(context: RoutingContext) {
        val response = context.response().putHeader("Access-Control-Allow-Origin", "*")
        val nickname = context.request().getParam(NICKNAME)
        val messageId = context.request().getParam(MESSAGE_ID)
        val token = context.request().getHeader(AUTHORIZATION)

        /* First of all, check authentication token: */
        val queryAuth = json { obj(
            DEFAULT_ID to nickname,
            TOKEN to token
        ) }
        MongoClient.createNonShared(vertx, MONGO_CONFIG).find(CONNECTIONS_COLLECTION, queryAuth) { findOperation ->
            when {
                findOperation.succeeded() -> {
                    if (findOperation.result().isEmpty()) {
                        response.setStatusCode(BAD_REQUEST.code()).end()
                    } else { /* Authentication OK, check for my messages */
                        val queryMessages = json { obj(
                            MESSAGE_ID to messageId,
                            RECIPIENT to nickname
                        ) }

                        MongoClient.createNonShared(vertx, MONGO_CONFIG).removeDocument(MESSAGES_COLLECTION, queryMessages) { findOperation2 ->
                            when {
                                findOperation2.succeeded() -> {
                                    response.setStatusCode(NO_CONTENT.code()).end()
                                }
                                findOperation2.failed() -> {
                                    response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                                }
                            }
                        }
                    }
                }
                findOperation.failed() -> {
                    response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                }
            }
        }
    }

    fun retrieveOldestMessage(context: RoutingContext) {
        retrieveOrderedMessage(context, 1)
    }

    fun retrieveLatestMessage(context: RoutingContext) {
        retrieveOrderedMessage(context, -1)
    }

    private fun retrieveOrderedMessage(context: RoutingContext, order: Int) {
        val response = context.response().putHeader("Access-Control-Allow-Origin", "*")
        val nickname = context.request().getParam(NICKNAME)
        val token = context.request().getHeader(AUTHORIZATION)

        /* First of all, check authentication token: */
        val queryAuth = json { obj(
            DEFAULT_ID to nickname,
            TOKEN to token
        ) }
        MongoClient.createNonShared(vertx, MONGO_CONFIG).find(CONNECTIONS_COLLECTION, queryAuth) { findOperation ->
            when {
                findOperation.succeeded() -> {
                    if (findOperation.result().isEmpty()) {
                        response.setStatusCode(BAD_REQUEST.code()).end()
                    } else { /* Authentication OK, check for my oldest message */
                        val queryMessages = json { obj(
                            RECIPIENT to nickname
                        ) }
                        val options = FindOptions(json { obj(
                            "limit" to 1,
                            "sort" to json { obj("datetime" to order) }
                        ) })
                        MongoClient.createNonShared(vertx, MONGO_CONFIG).findWithOptions(MESSAGES_COLLECTION, queryMessages, options) { findOperation2 ->
                            when {
                                findOperation2.succeeded() -> {
                                    val results2: List<JsonObject> = findOperation2.result()
                                    if (results2.isEmpty()) {
                                        response.setStatusCode(NO_CONTENT.code()).end()
                                    } else {
                                        val responseBody = json { obj(
                                            "id" to results2[0][DEFAULT_ID],
                                            "sender" to results2[0][SENDER],
                                            "content" to results2[0][CONTENT]
                                        ) }
                                        response.setStatusCode(OK.code()).end(Json.encodePrettily(responseBody))
                                    }
                                }
                                findOperation2.failed() -> {
                                    response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                                }
                            }
                        }
                    }
                }
                findOperation.failed() -> {
                    response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                }
            }
        }
    }
}
