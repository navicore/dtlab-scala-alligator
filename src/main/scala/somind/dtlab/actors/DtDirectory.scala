package somind.dtlab.actors

import akka.persistence._
import com.typesafe.scalalogging.LazyLogging
import somind.dtlab.models._
import somind.dtlab.observe.Observer

object DtDirectory extends LazyLogging {
  def name: String = this.getClass.getName
}

class DtDirectory extends DtPersistentActorBase[DtTypeMap] {

  override var state: DtTypeMap = DtTypeMap(types = Map())

  def validateRelationships(path: DtPath): DtResult = {
    val errs = path
      .relationships()
      .flatMap(i => {
        val (parent, child) = i
        state.types.get(parent) match {
          case Some(dtType) if dtType.children.exists(_.contains(child)) =>
            None
          case _ =>
            Some(DtErr(s"$parent type does not have children of type $child"))
        }
      })
    if (errs.nonEmpty)
      errs.head
    else
      DtOk()
  }

  def isValidTelemetry(m: TelemetryMsg): DtResult = {
    validateRelationships(m.p) match {
      case DtOk() =>
        val dtType = m.path().endTypeName()
        state.types.get(dtType) match {
          case Some(t) =>
            if (m.c.idx >= t.props.getOrElse(List()).length || m.c.idx < 0)
              DtErr("idx out of bounds")
            else
              DtOk()
          case _ =>
            DtErr("type not defined")
        }
      case e =>
        e
    }
  }

  def isValid(m: DtMsg[Any]): DtResult = {
    validateRelationships(m.path()) match {
      case DtOk() =>
        val dtType = m.path().endTypeName()
        state.types.get(dtType) match {
          case Some(_) =>
            DtOk()
          case _ =>
            DtErr("type not defined")
        }
      case e =>
        e
    }
  }

  override def receiveCommand: Receive = {

    case m: TelemetryMsg if m.path().trail.nonEmpty =>
      isValidTelemetry(m) match {
        case DtOk() =>
          upsert(m)
        case e =>
          sender ! e
      }

    case m: DtMsg[Any @unchecked] if m.path().trail.nonEmpty =>
      isValid(m) match {
        case DtOk() =>
          upsert(m)
        case e =>
          sender ! e
      }

    case dt: DtType =>
      state.types.get(dt.name) match {
        case Some(prev) =>
          sender ! Some(prev)
        case _ =>
          state = DtTypeMap(state.types + (dt.name -> dt))
          persistAsync(dt) { _ =>
            sender ! Some(dt)
            takeSnapshot()
          }
      }

    case del: DeleteDtType =>
      state.types.get(del.typeId) match {
        case Some(_) =>
          state = DtTypeMap(state.types - del.typeId)
          persistAsync(del) { _ =>
            sender ! DtOk()
            takeSnapshot()
          }
        case _ =>
          sender ! None
      }

    case typeId: String =>
      state.types.get(typeId) match {
        case Some(dt) =>
          sender ! Some(dt)
        case _ =>
          sender ! None
      }

    case _: SaveSnapshotSuccess =>
    case m =>
      logger.warn(s"unexpected message: $m")
      sender ! None

  }

  override def receiveRecover: Receive = {

    case dt: DtType =>
      state = DtTypeMap(state.types + (dt.name -> dt))
      Observer("reapplied_type_actor_command_from_jrnl")

    case del: DeleteDtType =>
      state = DtTypeMap(state.types - del.typeId)
      Observer("reapplied_type_actor_command_delete_from_jrnl")

    case SnapshotOffer(_, s: DtTypeMap @unchecked) =>
      Observer("recovered_type_actor_state_from_snapshot")
      state = s

    case _: RecoveryCompleted =>
      Observer("resurrected_type_actor")
      logger.debug(s"${self.path}: Recovery completed. State: $state")

    case x =>
      logger.warn(s"unexpected recover msg: $x")

  }

}
