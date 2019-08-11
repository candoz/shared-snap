package service

import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import org.yaml.snakeyaml.Yaml;

class SharedSnap : AbstractVerticle() {

    override fun start() {
        val yaml = Yaml()
        val inputStream = this::class.java
            .classLoader
            .getResourceAsStream("config.yaml")
        val obj = yaml.load<Map<String, Any>>(inputStream)
        val port = obj["PORT"] as Int
        val host = obj["HOST"] as String
        vertx
            .createHttpServer()
            .requestHandler(createRouter())
            .listen(port, host)
        println("Service ready on port $port and host $host")
    }

    private fun createRouter(): Router {
        RequestManager.initializeRequestManager(vertx)
        return Router.router(vertx).apply {
            route().handler(BodyHandler.create())
            options(CONNECTIONS_PATH).handler { RequestManager.optionsCors(it) }
            post(CONNECTIONS_PATH).handler { RequestManager.createConnection(it) }
            delete(CONNECTIONS_PATH).handler { RequestManager.deleteConnection(it) }
            options(MESSAGES_PATH).handler { RequestManager.optionsCors(it) }
            post(MESSAGES_PATH).handler { RequestManager.createMessage(it) }
            get(MESSAGES_PATH).handler { RequestManager.retrieveMessages(it) }
            delete(MESSAGES_PATH).handler { RequestManager.deleteMessages(it) }
            options(OLDEST_MESSAGE_PATH).handler { RequestManager.optionsCors(it) }
            get(OLDEST_MESSAGE_PATH).handler { RequestManager.retrieveOldestMessage(it) }
            options(LATEST_MESSAGE_PATH).handler { RequestManager.optionsCors(it) }
            get(LATEST_MESSAGE_PATH).handler { RequestManager.retrieveLatestMessage(it) }
            options(SINGLE_MESSAGE_PATH).handler { RequestManager.optionsCors(it) }
            delete(SINGLE_MESSAGE_PATH).handler { RequestManager.deleteSingleMessage(it) }
        }
    }

    companion object {
        private const val CONNECTIONS_PATH = "/v1/connections/:nickname"
        private const val MESSAGES_PATH = "/v1/messages/:nickname"
        private const val OLDEST_MESSAGE_PATH = "$MESSAGES_PATH/oldest"
        private const val LATEST_MESSAGE_PATH = "$MESSAGES_PATH/latest"
        private const val SINGLE_MESSAGE_PATH = "$MESSAGES_PATH/:messageId"
    }
}