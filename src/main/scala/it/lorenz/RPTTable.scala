package it.lorenz

trait RPTTable {
  self: DBComponent =>

  import driver.api._

  case class RPT(idDominio: String, iuv: String, ccp: String, canale: String)

  class RPTTable(tag: Tag) extends Table[RPT](tag, "RPT") {
    def ccp = column[String]("CCP")

    def idDominio = column[String]("IDENT_DOMINIO")

    def iuv = column[String]("IUV")

    def canale = column[String]("CANALE")

    def * = (idDominio, iuv, ccp, canale) <> (RPT.tupled, RPT.unapply)

  }

  def rptTable = TableQuery[RPTTable]
}
