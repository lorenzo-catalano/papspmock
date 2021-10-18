import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.reflect.io.File
import scala.xml.XML

object Main extends cask.MainRoutes{
  override def port: Int = 8087
  override def host: String = "0.0.0.0"

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
    "mock server up"
  }

  @cask.post("/", subpath = true)
  def doThing(request: cask.Request) = {
    val xml = XML.loadString(new String(request.readAllBytes()))
    val primitiva = (xml \\ "Body" \ "_").head.label
    println(primitiva)
    val path = Paths.get(s"/mocks/${primitiva}/OK.xml")
    val resString = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    primitiva match {
      case "paGetPaymentReq" =>
        val nn =  (xml \\ "noticeNumber").text
        val (iuv,_,_,_) = getNoticeNumberData(nn)
        resString.replace("{CREDITORREFERENCEID}",iuv)
      case _ =>
        resString
    }


  }

  initialize()

}