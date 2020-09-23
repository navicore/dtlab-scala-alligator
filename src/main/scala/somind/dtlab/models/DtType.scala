package somind.dtlab.models

import java.time.ZonedDateTime

//
// DTTYPE BEGIN
//

final case class DeleteDtType(typeId: String)

// particular type of a kind
final case class DtType(
    // the name of our type
    name: String,
    // the names of the properties (called props instead of attributes because
    // values of props can change - values of attributes cannot change)
    props: Option[Seq[String]],
    // the ids/names of the types of child actors that this actor type can instantiate
    children: Option[Set[String]],
    // datetime of creation - no updates allowed
    created: ZonedDateTime = ZonedDateTime.now()
)

// for API to avoid setting created
final case class LazyDtType(
    props: Option[Seq[String]],
    children: Option[Set[String]],
    created: Option[ZonedDateTime]
) {
  def dtType(name: String): DtType =
    DtType(name, props, children, created.getOrElse(ZonedDateTime.now()))
}

// collection of all types in domain
final case class DtTypeMap(
    types: Map[String, DtType]
)

//
// DTTYPE END
//
