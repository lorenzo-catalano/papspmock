package it.lorenz

import slick.jdbc.JdbcBackend.Database
import slick.jdbc.OracleProfile
import slick.util.AsyncExecutor

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }
import java.time.{ LocalDate, LocalDateTime }
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.xml.XML

object Server extends cask.MainRoutes {
  override def port: Int = sys.env.get("MOCKPORT").map(_.toInt).getOrElse(8087)
  override def host: String = "0.0.0.0"

  val basepath: String = new File(".").getCanonicalPath

  var countChiediRT = new AtomicInteger(0)
  var countAckRT = new AtomicInteger(0)

  val db = Database.forURL(
    "jdbc:oracle:thin:@//host.k3d.internal:1521/ORCLCDB.localdomain",
    driver = "oracle.jdbc.driver.OracleDriver",
    user = "NODO_ONLINE",
    password = "NODO_ONLINE",
    executor = AsyncExecutor("test1", minThreads = 10, queueSize = 1000, maxConnections = 10, maxThreads = 10)
  )
  val onlineRepository = OnlineRepository(OracleProfile, db)

  def getNoticeNumberData(noticeNumber: String): (String, Option[Long], Option[Int], Option[Long]) = {
    val auxDigit = noticeNumber.substring(0, 1).toLong
    val segregazione = auxDigit match {
      case 3 | 4 => Some(noticeNumber.substring(1, 3).toLong)
      case _     => None
    }
    val progressivo = auxDigit match {
      case 0 => Some(noticeNumber.substring(1, 3).toInt)
      case _ => None
    }
    val auxValue = auxDigit.toLong match {
      case 0 | 3 => None
      case _     => Option(auxDigit.toLong)
    }
    val iuv = if (auxDigit == 0) {
      noticeNumber.substring(3)
    } else {
      noticeNumber.substring(1)
    }
    (iuv, segregazione, progressivo, auxValue)
  }

  @cask.get("/alive")
  def hello() = {
    s"""OK"""
  }

  @cask.post("/", subpath = true)
  def doThing(request: cask.Request): cask.Response[String] = {
    val action = request.headers.get("soapaction").map(_.head)
    println(s"/${request.remainingPathSegments.mkString("/")}")
    val payload = new String(request.readAllBytes())

    val tcid = request.headers.get("tcid").map(_.head) match {
      case Some(value) if value.contains("timeout_") =>
        val t = value.split("_")(1).toLong
        println(s"sleeping ${t}")
        Thread.sleep(t)
        Some(value.split("_")(3))
      case Some(v) => Some(v)
      case None    => Some("OK")
    }

    val (folder, tipo) = if (action.isEmpty) {
      request.remainingPathSegments.last -> "json"
    } else {
      action.get.replaceAll("\"", "") -> "xml"
    }

    if (action.contains("\"pspNotifyPayment\"") || folder == "sendpaymentresult") {
      cask.Response("", statusCode = 404)
//      Thread.sleep(11000)
    } else {
      println(s"${action.getOrElse("no action")} - ${LocalDateTime.now()}")

      val path = Paths.get(s"${basepath}/mocks/${folder}/${tcid.getOrElse("OK")}.$tipo")

      folder match {
        case "pspChiediRT" =>
          val xml = XML.loadString(payload)
          val dom = (xml \\ "identificativoDominio").text
          val iuv = (xml \\ "identificativoUnivocoVersamento").text
          val ccp = (xml \\ "codiceContestoPagamento").text
          val pathrt = Paths.get(s"$basepath/mocks/pspChiediRT/rt.xml")
          val rt = new String(Files.readAllBytes(pathrt), StandardCharsets.UTF_8)
          val rtrep = rt.replace("{pa}", dom).replace("{iuv}", iuv).replace("{ccp}", ccp)
          val resString = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
          val resStringUpd = resString.replace("{RT}", Base64.getEncoder.encodeToString(rtrep.getBytes))

          cask.Response(resStringUpd, statusCode = 200)
        case "pspChiediListaRT" =>
          val xml = XML.loadString(payload)
          val canale = (xml \\ "identificativoCanale").text
          val rpts = Await.result(onlineRepository.getForChiediLista(canale), Duration.Inf)

          val sss = rpts.map(s => {
            <elementoListaRTResponse>
              <identificativoDominio>{s.idDominio}</identificativoDominio>
              <identificativoUnivocoVersamento>{s.iuv}</identificativoUnivocoVersamento>
              <codiceContestoPagamento>{s.ccp}</codiceContestoPagamento>
            </elementoListaRTResponse>
          })

          println(s"sending response pspChiediListaRT canale $canale")

          val resString = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)

          val resStringUpd = resString.replace("{ELEMENTI}", sss.mkString("\n"))

          cask.Response(resStringUpd, statusCode = 200)
        case "paGetPayment" | "paGetPaymentV2" =>
          val xml = XML.loadString(payload)
          val nn = (xml \\ "noticeNumber").text
          val (iuv, _, _, _) = getNoticeNumberData(nn)

          cask.Response(new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replace("{CREDITORREFERENCEID}", iuv), statusCode = 200)
        case _ =>
          if (path.toFile.exists()) {

            cask.Response(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), statusCode = 200)
          } else {

            cask.Response(s"not found ${path}", statusCode = 200)
          }
      }
    }

    /*countChiediRT.incrementAndGet()
    Try({
      val xml = XML.loadString(payload)
      println(s"${action.getOrElse("no action")} - ${LocalDateTime.now()}")
      if (action.contains("\"paaInviaRT\"")) {
        println("sleeping")
        Thread.sleep(20000)
      }

      val primitiva = (xml \\ "Body" \ "_").head.label

      val path = Paths.get(s"${basepath}/mocks/${primitiva}/${tcid.getOrElse("OK")}.xml")
//      println(s"${LocalDateTime.now()} $primitiva(${action}) -> ${primitiva}/${tcid.getOrElse("OK")}.xml")
      if (path.toFile.exists()) {
        val resString = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        primitiva match {
          case "pspChiediListaRT" => {

            val canale = (xml \\ "identificativoCanale").text
            val rpts = Await.result(onlineRepository.getForChiediLista(canale), Duration.Inf)

            val sss = rpts.map(s => {
              <elementoListaRTResponse>
                <identificativoDominio>{s.idDominio}</identificativoDominio>
                <identificativoUnivocoVersamento>{s.iuv}</identificativoUnivocoVersamento>
                <codiceContestoPagamento>{s.ccp}</codiceContestoPagamento>
              </elementoListaRTResponse>
            })

            println(s"sending response pspChiediListaRT canale $canale")
            resString.replace("{ELEMENTI}", sss.mkString("\n"))
          }

          case "pspChiediRT" =>
            val dom = (xml \\ "identificativoDominio").text
            val iuv = (xml \\ "identificativoUnivocoVersamento").text
            val ccp = (xml \\ "codiceContestoPagamento").text
            val pathrt = Paths.get(s"$basepath/mocks/pspChiediRT/rt.xml")
            val rt = new String(Files.readAllBytes(pathrt), StandardCharsets.UTF_8)
            val rtrep = rt.replace("{pa}", dom).replace("{iuv}", iuv).replace("{ccp}", ccp)
            val c = countChiediRT.incrementAndGet()
            val canale = request.headers.get("canale").flatMap(_.headOption)
//            println(s"sending response pspChiediRT canale:${canale.getOrElse("n/d")} $c $iuv")
            println(s"insert into SERVER (iuv,messagetype) values ('$iuv','pspChiediRT');")
            resString.replace("{RT}", Base64.getEncoder.encodeToString(rtrep.getBytes))

          case "paGetPaymentReq" =>
            val nn = (xml \\ "noticeNumber").text
            val (iuv, _, _, _) = getNoticeNumberData(nn)
            resString.replace("{CREDITORREFERENCEID}", iuv)

          case r =>
            if (r == "pspInviaAckRT") {
              countAckRT.incrementAndGet()
              val iuv = (xml \\ "identificativoUnivocoVersamento").text
              println(s"insert into SERVER (iuv,messagetype) values ('$iuv','pspChiediRT');")
            }
            resString
        }
      } else {
        println(s"not found ${path}")
        s"not found ${path}"
      }
    }).getOrElse("Error")*/

  }

  initialize()

}
