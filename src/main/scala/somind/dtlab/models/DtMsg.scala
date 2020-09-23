package somind.dtlab.models

import java.time.ZonedDateTime

//
// DTMSG BEGIN
//

sealed trait DtResult
final case class DtOk() extends DtResult
final case class DtErr(message: String) extends DtResult

sealed trait DtMsg[+T] {
  def path(): DtPath
  def content(): T
  def trailMsg(): DtMsg[T]
}

//
// DTMSG END
//

//
// DTSTATE BEGIN
//

// collection of all props in an actor instance
final case class DtState(
    state: Map[Int, Telemetry] = Map()
)

final case class DtGetState(p: DtPath) extends DtMsg[Any] {
  override def path(): DtPath = p
  override def content(): Any = None
  def trailMsg(): DtGetState = p.trail match {
    case Some(tp) => DtGetState(tp)
    case _        => this
  }
}

//
// DTSTATE END
//

//
// TELEMETRY BEGIN
//

final case class LazyTelemetry(
    idx: Int,
    value: Double,
    datetime: Option[ZonedDateTime]
) {
  def telemetry(): Telemetry =
    Telemetry(idx, value, datetime.getOrElse(ZonedDateTime.now()))
}

final case class Telemetry(
    idx: Int,
    value: Double,
    datetime: ZonedDateTime = ZonedDateTime.now()
)

final case class LazyNamedTelemetry(
    name: String,
    value: Double,
    datetime: Option[ZonedDateTime]
) {
  def telemetry(): NamedTelemetry =
    NamedTelemetry(name, value, datetime.getOrElse(ZonedDateTime.now()))
}

final case class NamedTelemetry(
    name: String,
    value: Double,
    datetime: ZonedDateTime = ZonedDateTime.now()
)

final case class TelemetryMsg(p: DtPath, c: Telemetry)
    extends DtMsg[Telemetry] {
  def path(): DtPath = p
  def content(): Telemetry = c
  def trailMsg(): DtMsg[Telemetry] = p.trail match {
    case Some(tp) =>
      TelemetryMsg(tp, c)
    case _ => this
  }
}

//
// TELEMETRY END
//

//
// LINK BEGIN
//

// source and dest of a link
sealed trait DtLinkPath
final case class DtLinkSource(path: DtPath) extends DtLinkPath
final case class DtLinkDest(path: DtPath) extends DtLinkPath

final case class DtLink(source: DtLinkSource, dest: DtLinkDest)
final case class UnDtLink(dtPath: DtPath, dest: DtLinkDest)

final case class DtLinkMsg(p: DtPath, l: DtLink) extends DtMsg[DtLink] {
  def path(): DtPath = p
  def content(): DtLink = l
  def trailMsg(): DtMsg[DtLink] = p.trail match {
    case Some(tp) =>
      DtLinkMsg(tp, l)
    case _ => this
  }
}

// collection of all links in an actor instance
// key is destId
final case class DtLinkMap(links: Map[DtLinkDest, DtLink])

//
// LINK END
//
