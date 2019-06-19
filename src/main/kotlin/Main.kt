import io.vertx.core.Vertx
import service.SharedSnap

object Main {
    @JvmStatic
    fun main(args: Array<String>) { Vertx.vertx().deployVerticle(SharedSnap()) }
}
