package it.lorenz

import java.time.LocalDateTime

trait StatiRPTTable {
  self: DBComponent =>

  import driver.api._

  class StatiRPTSnapshot(tag: Tag) extends Table[StatoRPTSnapshot](tag, "STATI_RPT_SNAPSHOT") {
    def idSessione = column[String]("ID_SESSIONE")

    def idDominio = column[String]("ID_DOMINIO")

    def iuv = column[String]("IUV")

    def ccp = column[String]("CCP")

    def stato = column[String]("STATO")

    def insertedTimestamp = column[LocalDateTime]("INSERTED_TIMESTAMP")

    def updatedTimestamp = column[LocalDateTime]("UPDATED_TIMESTAMP")

    def insertedBy = column[String]("INSERTED_BY")

    def updatedBy = column[String]("UPDATED_BY")

    def push = column[Option[Int]]("PUSH")

    override def * =
      (
        idSessione,
        idDominio,
        iuv,
        ccp,
        stato,
        insertedTimestamp,
        updatedTimestamp,
        insertedBy,
        updatedBy,
        push) <> (StatoRPTSnapshot.tupled, StatoRPTSnapshot.unapply)
  }

  def statiRPTSnapshotTable = TableQuery[StatiRPTSnapshot]
}
