package vct.options

sealed trait Backend

case object Backend extends ReadEnum[Backend] {
  override val options: Map[String, Backend] = Map(
    "silicon" -> Silicon,
    "carbon" -> Carbon,
    "VeyMont" -> VeyMont
  )

  case object Silicon extends Backend
  case object Carbon extends Backend
  case object VeyMont extends Backend
}