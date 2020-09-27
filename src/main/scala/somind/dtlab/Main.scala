package somind.dtlab

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.typesafe.scalalogging.LazyLogging
import somind.dtlab.Conf._
import somind.dtlab.models.JsonSupport
import somind.dtlab.observe.ObserverRoute
import somind.dtlab.routes._

object Main extends LazyLogging with JsonSupport with HttpSupport {

  def main(args: Array[String]) {

    val route =
      ObserverRoute.apply ~
        handleErrors {
          cors(corsSettings) {
            handleErrors {
              logRequest(urlpath) {
                pathPrefix(urlpath) {
                  ignoreTrailingSlash {
                    TypeApiRoute.apply ~
                      TelemetryApiRoute.apply("actor") ~
                      LinkApiRoute.apply("link") ~
                      OperatorApiRoute.apply
                  }
                }
              }
            }
          }
        }

    Http().newServerAt("0.0.0.0", port).bindFlow(route)
  }
}
