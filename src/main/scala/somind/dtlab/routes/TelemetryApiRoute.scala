package somind.dtlab.routes

import akka.http.scaladsl.server._
import com.typesafe.scalalogging.LazyLogging
import somind.dtlab.HttpSupport
import somind.dtlab.models._

/**
  * Enables CRUD for actors and their states.
  *
  * Actors are automatically created if you post telemetry to
  * them.  If they already exist, the new telemetry gets added
  * to their journal and is reflected in the current state view.
  *
  * (When implemented), DELETE will remove the journals of the actor.
  *
  * Telemetry may be expressed in 3 ways:
  *   1. indexed (the native internal format)
  *   2. named
  *   3. pathed
  *
  * When addressing an actor, suffix the path with named or pathed to
  * use the named or pathed telemetry model.
  *
  */
object TelemetryApiRoute
    extends ActorRouteBase[TelemetryMsg]
    with JsonSupport
    with LazyLogging
    with Directives
    with HttpSupport {}
