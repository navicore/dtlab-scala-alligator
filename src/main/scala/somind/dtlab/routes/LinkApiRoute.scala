package somind.dtlab.routes

import akka.http.scaladsl.server.Directives
import com.typesafe.scalalogging.LazyLogging
import somind.dtlab.HttpSupport
import somind.dtlab.models._

object LinkApiRoute
    extends ActorRouteBase[DtLinkMsg]
    with JsonSupport
    with LazyLogging
    with Directives
    with HttpSupport {}
