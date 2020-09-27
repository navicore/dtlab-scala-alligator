package somind.dtlab.actors

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging
import somind.dtlab.models.{DtLinkMap, DtMsg}

trait DtActorBase extends Actor with LazyLogging {

  var links: DtLinkMap = DtLinkMap(Map())

  def create(m: DtMsg[Any], name: String): Unit =
    context.actorOf(Props[DtActor], name) forward m

  def upsert(m: DtMsg[Any]): Unit = {

    m.path().trail match {
      case Some(p) =>
        val instanceId = p.instanceId
        if (self.path.name == p.typeId)
          context
            .child(instanceId)
            .fold(create(m.trailMsg(), instanceId))(_ forward m.trailMsg())
        else
          context.child(p.typeId).fold(create(m, p.typeId))(_ forward m)
      case e =>
        throw new UnsupportedOperationException(s"no trail: $e")
    }

  }

}
