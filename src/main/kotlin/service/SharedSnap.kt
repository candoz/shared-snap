package service

import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

class SharedSnap : AbstractVerticle() {

    override fun start() {
        vertx
            .createHttpServer()
            .requestHandler(createRouter())
            .listen(PORT, HOST)
        println("Service ready on port $PORT and host $HOST")
    }

    private fun createRouter(): Router {
        RequestManager.initializeRequestManager(vertx)
        return Router.router(vertx).apply {
            route().handler(BodyHandler.create())
            post(CONNECTIONS_PATH).handler { RequestManager.createConnection(it) }
            delete(CONNECTIONS_PATH).handler { RequestManager.deleteConnection(it) }
            post(MESSAGES_PATH).handler { RequestManager.createMessage(it) }
            get(MESSAGES_PATH).handler { RequestManager.retrieveMessages(it) }
            delete(MESSAGES_PATH).handler { RequestManager.deleteMessages(it) }
            get(OLDEST_MESSAGE_PATH).handler { RequestManager.retrieveOldestMessage(it) }
            get(LATEST_MESSAGE_PATH).handler { RequestManager.retrieveLatestMessage(it) }
            delete(SINGLE_MESSAGE_PATH).handler { RequestManager.deleteSingleMessage(it) }
        }
    }

    companion object {
        private const val PORT = 10000
        private const val HOST = "localhost"
        private const val CONNECTIONS_PATH = "/v1/connections/:nickname"
        private const val MESSAGES_PATH = "/v1/messages/:nickname"
        private const val OLDEST_MESSAGE_PATH = "$MESSAGES_PATH/oldest"
        private const val LATEST_MESSAGE_PATH = "$MESSAGES_PATH/latest"
        private const val SINGLE_MESSAGE_PATH = "$MESSAGES_PATH/:messageId"
    }
}