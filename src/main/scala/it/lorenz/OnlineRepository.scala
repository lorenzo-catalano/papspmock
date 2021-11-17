package it.lorenz

import slick.jdbc.{JdbcBackend, JdbcProfile}

import scala.concurrent.{ExecutionContext, Future}

case class OnlineRepository(override val driver: JdbcProfile, override val db: JdbcBackend#DatabaseDef)(implicit ec: ExecutionContext)
  extends DBComponent
    with RPTTable
    with StatiRPTTable {

  import driver.api._

  def getForChiediLista(idCanale: String): Future[Seq[StatoRPTSnapshot]] = {
    val action = rptTable.join(statiRPTSnapshotTable).on((d, s) => {
      d.idDominio === s.idDominio && d.ccp === s.ccp && d.iuv === s.iuv
    }).filter(_._1.canale === idCanale).map(_._2)
    db.run(
      action.result
    )
  }

}
