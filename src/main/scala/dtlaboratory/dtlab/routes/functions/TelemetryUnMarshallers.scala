package dtlaboratory.dtlab.routes.functions

import akka.pattern._
import com.typesafe.scalalogging.LazyLogging
import dtlaboratory.dtlab.models._
import dtlaboratory.dtlab.Conf._

import scala.concurrent.Future

/**
 * some ugliness to allow the APIs to use verbose type field names instead of indexes of fields
 */
object TelemetryUnMarshallers extends JsonSupport with LazyLogging {

  type UnMarshaller =
    (Option[NamedTelemetry], DtPath) => Future[Option[Telemetry]]

  def makeTelemetry(names: List[String],
                    ntelem: NamedTelemetry): Option[Telemetry] = {
    val idx = names.indexOf(ntelem.name.split('.').toList.last)
    if (idx >= 0)
      Some(Telemetry(idx, ntelem.value, ntelem.datetime))
    else None
  }

  def NamedUnMarshaller(t: Option[NamedTelemetry],
                        dtp: DtPath): Future[Option[Telemetry]] = {
    t match {
      case Some(ntelem) =>
        val f = dtDirectory ask dtp.endTypeName()
        f.map((r: Any) => {
          r match {
            case Some(dt: DtType) if dt.props.nonEmpty =>
              val names: List[String] = dt.props.getOrElse(Set()).toList
              makeTelemetry(names, ntelem)
            case _ => None
          }
        })
      case _ => Future { None }
    }

  }

}
