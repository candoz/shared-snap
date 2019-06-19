package service

import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

object RequestManager {
    private var vertx: Vertx? = null
    private val CONFIG = json {
        obj(
            "connection_string" to "mongodb://localhost:12345/snapdb")
    }

    fun initializeRequestManager(vertx: Vertx) {
        RequestManager.vertx = vertx
    }

    fun createConnection(context: RoutingContext?) {
    }

    fun deleteConnection(context: RoutingContext?) {
    }

    fun createMessage(context: RoutingContext?) {
    }

    fun retrieveMessages(context: RoutingContext?) {
    }

    fun deleteMessages(context: RoutingContext?) {
    }

    fun deleteSingleMessage(context: RoutingContext?) {
    }

    fun retrieveOldestMessage(context: RoutingContext?) {
    }

    fun retrieveLatestMessage(context: RoutingContext?) {
    }
}
