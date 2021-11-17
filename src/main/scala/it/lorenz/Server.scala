package it.lorenz

import slick.jdbc.JdbcBackend.Database
import slick.jdbc.OracleProfile
import slick.util.AsyncExecutor

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.LocalDateTime
import java.util.Base64
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.xml.XML


object Server extends cask.MainRoutes{
  override def port: Int = sys.env.get("MOCKPORT").map(_.toInt).getOrElse(8087)
  override def host: String = "0.0.0.0"


  val db = Database.forURL(
    "jdbc:oracle:thin:@//host.k3d.internal:1521/ORCLCDB.localdomain",
    driver = "oracle.jdbc.driver.OracleDriver",
    user="NODO_ONLINE",
    password = "NODO_ONLINE",
    executor = AsyncExecutor("test1", minThreads = 10, queueSize = 1000, maxConnections = 10, maxThreads = 10)
  )
  val onlineRepository = OnlineRepository(OracleProfile, db)

  def getNoticeNumberData(noticeNumber:String): (String, Option[Long], Option[Int], Option[Long]) ={
    val auxDigit = noticeNumber.substring(0,1).toLong
    val segregazione = auxDigit match {
      case 3 | 4 => Some(noticeNumber.substring(1,3).toLong)
      case _ => None
    }
    val progressivo = auxDigit match {
      case 0 => Some(noticeNumber.substring(1,3).toInt)
      case _ => None
    }
    val auxValue = auxDigit.toLong match {
      case 0 | 3 => None
      case _ => Option(auxDigit.toLong)
    }
    val iuv = if(auxDigit==0){
      noticeNumber.substring(3)
    }else{
      noticeNumber.substring(1)
    }
    (iuv,segregazione,progressivo,auxValue)
  }


  @cask.get("/")
  def hello() = {
    println(s"/ ${LocalDateTime.now()}")
    """{"name":"addios"}"""
  }

  @cask.post("/", subpath = true)
  def doThing(request: cask.Request) = {
    //    Thread.sleep(132000)
    val payload = new String(request.readAllBytes())
    val xml = XML.loadString(payload)
    val tcid = request.headers.get("tcid").map(_.head)
    val action = request.headers.get("soapaction").map(_.head)

    val primitiva = (xml \\ "Body" \ "_").head.label

    val path = Paths.get(s"/mocks/${primitiva}/${tcid.getOrElse("OK")}.xml")
    println(s"${LocalDateTime.now()} $primitiva(${action}) -> ${primitiva}/${tcid.getOrElse("OK")}.xml")
    if(path.toFile.exists()){
      val resString = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
      primitiva match {
        case "pspChiediListaRT" => {

          val canale = (xml \\ "identificativoCanale").text
          val rpts = Await.result(onlineRepository.getForChiediLista(canale),Duration.Inf)
          val sss = rpts.map(s=>{
            <elementoListaRTResponse>
              <identificativoDominio>{s.idDominio}</identificativoDominio>
              <identificativoUnivocoVersamento>{s.iuv}</identificativoUnivocoVersamento>
              <codiceContestoPagamento>{s.ccp}</codiceContestoPagamento>
            </elementoListaRTResponse>
          })
          resString.replace("{ELEMENTI}",sss.mkString("\n"))
        }

        case "pspChiediRT" =>
          Thread.sleep((Math.random()*500).toLong)
          val dom =(xml \\ "identificativoDominio").text
          val iuv = (xml \\ "identificativoUnivocoVersamento").text
          val ccp = (xml \\ "codiceContestoPagamento").text
          val pathrt = Paths.get(s"/mocks/pspChiediRT/rt.xml")
          val rt = new String(Files.readAllBytes(pathrt), StandardCharsets.UTF_8)
          val rtrep = rt
            .replace("{pa}",dom)
            .replace("{iuv}",iuv)
            .replace("{ccp}",ccp)
          resString.replace("{RT}",Base64.getEncoder.encodeToString(rtrep.getBytes))

        case "paGetPaymentReq" =>
          val nn =  (xml \\ "noticeNumber").text
          val (iuv,_,_,_) = getNoticeNumberData(nn)
          resString.replace("{CREDITORREFERENCEID}",iuv)

        case _ =>
          resString
      }
    }else{
      println(s"not found ${path}")
      s"not found ${path}"
    }

  }

  initialize()

}