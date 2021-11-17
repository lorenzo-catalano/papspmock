package it.lorenz

import java.time.LocalDateTime

case class StatoRPTSnapshot(
                             sessionId: String,
                             idDominio: String,
                             iuv: String,
                             ccp: String,
                             stato: String,
                             insertedTimestamp: LocalDateTime,
                             updatedTimestamp: LocalDateTime,
                             insertedBy: String,
                             updatedBy: String,
                             push: Option[Int] = None) {

}
