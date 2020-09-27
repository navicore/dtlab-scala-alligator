package somind.dtlab.actors

import akka.persistence._
import com.typesafe.scalalogging.LazyLogging
import somind.dtlab.models._
import somind.dtlab.observe.Observer

object DtActor extends LazyLogging {
  def name: String = this.getClass.getName
}

class DtActor extends DtPersistentActorBase[DtState] {

  override var state: DtState = DtState()

  override def receiveCommand: Receive = {

    case m: DtMsg[Any @unchecked] if m.path().trail.nonEmpty =>
      upsert(m)

    case tm: TelemetryMsg =>
      state = DtState(state.state + (tm.c.idx -> tm.c))
      persistAsync(tm.c) { _ =>
        sender ! DtOk()
        takeSnapshot()
      }

    case lm: DtLinkMsg =>
      links = DtLinkMap(links.links + (lm.l.dest -> lm.l))
      persistAsync(lm.l) { _ =>
        sender ! DtOk()
        takeSnapshot()
      }

    case _: DtGetState =>
      sender ! state

    case _: SaveSnapshotSuccess =>
    case m =>
      logger.warn(s"unexpected message: $m")
      sender ! None

  }

  override def receiveRecover: Receive = {

    case t: Telemetry =>
      state = DtState(state.state + (t.idx -> t))

    case l: DtLink =>
      links = DtLinkMap(links.links + (l.dest -> l))

    case SnapshotOffer(_, snapshot: (DtState, DtLinkMap) @unchecked) =>
      val (s, l) = snapshot
      state = s
      links = l
      Observer("recovered_dt_actor_state_from_snapshot")

    case SnapshotOffer(_, s: DtState @unchecked) => // deprecated todo: rm
      state = s
      Observer("recovered_dt_actor_state_from_snapshot")

    case _: RecoveryCompleted =>
      logger.debug(s"${self.path}: Recovery completed. State: $state Links: $links")
      Observer("resurrected_dt_actor")

    case x =>
      logger.warn(s"unexpected recover msg: $x")

  }

}
