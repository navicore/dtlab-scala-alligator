package somind.dtlab.models

//
// DTPATH BEGIN
//

object DtPath {
  def apply(segs: List[String]): Option[DtPath] = {
    segs match {
      case s if s.length < 2 => None
      case s =>
        Option(DtPath(s.head, s(1), DtPath(s.drop(2))))
    }
  }
}

case class DtPath(typeId: String,
                  instanceId: String,
                  trail: Option[DtPath] = None) {
  // convenience method to get the final typeName for validation
  def endTypeName(): String = {
    trail match {
      case Some(t) =>
        t.endTypeName()
      case _ => typeId
    }
  }
  def relationships(): List[(String, String)] = {
    trail match {
      case None                         => List()
      case Some(dt) if typeId == "root" => dt.relationships()
      case Some(dt)                     => (typeId, dt.typeId) :: dt.relationships()
    }
  }
  private def pathToString(p: DtPath): String = {
    s"/${p.typeId}/${p.instanceId}" + {
      p.trail match {
        case None =>
          ""
        case Some(t) =>
          pathToString(t)
      }
    }
  }
  override def toString: String = {
    pathToString(this)
  }
}

//
// DTPATH END
//
