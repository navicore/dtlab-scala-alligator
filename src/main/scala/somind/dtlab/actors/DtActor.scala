package somind.dtlab.actors

import akka.persistence._
import com.typesafe.scalalogging.LazyLogging
import somind.dtlab.models._
import somind.dtlab.observe.Observer
import somind.dtlab.operators.ApplyBuiltInOperator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object DtActor extends LazyLogging {
  def name: String = this.getClass.getName
}

class DtActor extends DtPersistentActorBase[DtState, Telemetry] {

  override var state: DtState = DtState()

  def applyOperator(t: Telemetry): Unit = {
    // find operators that list this t's index as input and apply them
    val ops = operators.operators.values.filter(_.input.contains(t.idx))
    ops.foreach(op => {
      ApplyBuiltInOperator(t, state, op) match {
        case Some(t) => self ! t
        case _       => // noop
      }
    })
  }

  def handleTelemetry(t: Telemetry): Unit = {
    state = DtState(state.state + (t.idx -> t))
    persistAsync(t) { _ =>
      takeSnapshot()
    }
  }

  def handleTelemetryMsg(tm: TelemetryMsg): Unit = {
    state = DtState(state.state + (tm.c.idx -> tm.c))
    val sndr = sender()
    persistAsync(tm.c) { _ =>
      takeSnapshot()
      if (sndr != self) {
        applyOperator(tm.c) // apply operators to effects that come from outside the DT
        sender ! DtOk()
      }
    }
  }

  def handleGetJrnl(m: GetJrnl): Unit = {
    val result = grabJrnl(m.limit, m.offset)
    val sndr = sender()
    result onComplete {
      case Success(r) =>
        sndr ! r
      case Failure(exception) =>
        sndr ! DtErr(exception.getMessage)
    }
  }

  override def receiveCommand: Receive = {

    case _: TakeSnapshot => takeSnapshot(true)

    case m: DtMsg[Any @unchecked]
        if m.path().trail.nonEmpty && !m
          .path()
          .trail
          .exists(leaf =>
            leaf.typeId == self.path.name && leaf.instanceId == "children" && leaf.trail.isEmpty) =>
      logger.debug(s"${self.path.name} forwarding $m")
      upsert(m)

    case m: GetJrnl => handleGetJrnl(m)

    case _: GetOperators => sender() ! operators

    case _: GetChildrenNames => sender ! children

    case _: DeleteOperators =>
      operators = OperatorMap()
      takeSnapshot(true)
      sender ! DtOk()

    case op: OperatorMsg =>
      operators = OperatorMap(operators.operators + (op.c.name -> op.c))
      takeSnapshot(true)
      sender ! operators

    // raw telemetry are derived inside the operators and sent from self
    case t: Telemetry => handleTelemetry(t)

    // telemetry msgs are sent with dtpath addressing wrappers and
    // are the entry point / triggers for all complex and simple state change processing
    case tm: TelemetryMsg => handleTelemetryMsg(tm)

    case _: GetState => sender ! state

    case _: SaveSnapshotSuccess =>
    case m =>
      logger.warn(s"unexpected message: $m")
      sender ! None

  }

  override def receiveRecover: Receive = {

    case t: Telemetry =>
      Try {
        DtState(state.state + (t.idx -> t))
      } match {
        case Success(s) =>
          state = s
          Observer("recovery_of_dt_actor_state_from_jrnl")
        case Failure(e) =>
          Observer("recovery_of_dt_actor_state_from_jrnl_failed")
          logger.error(s"can not recalculate state: $e", e)
      }

    case SnapshotOffer(_, snapshot: DtStateHolder[DtState] @unchecked) =>
      Try {
        (snapshot.state, snapshot.children, snapshot.operators)
      } match {
        case Success(s) =>
          state = s._1
          children = s._2
          operators = s._3
          Observer("recovered_dt_actor_state_from_snapshot")
        case Failure(e) =>
          Observer("recovery_of_dt_actor_state_from_snapshot_failed")
          logger.error(s"can not recover state: $e", e)
      }

    case _: RecoveryCompleted =>
      logger.debug(
        s"${self.path}: Recovery completed. State: $state Children: $children")
      Observer("resurrected_dt_actor")

    case x =>
      Observer("resurrected_dt_actor_unexpected_msg")
      logger.warn(s"unexpected recover msg: $x")

  }

}
