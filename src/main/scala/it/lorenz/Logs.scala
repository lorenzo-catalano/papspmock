//package it.lorenz
//
//object Logs extends App {
//
//  val files = Seq(
//    "prd-nodo-cron-job-pa-invia-rt-1636732440-nvtv9",
//    "prd-nodo-cron-job-pa-invia-rt-1636740480-9ksp7",
//    "prd-nodo-cron-job-pa-invia-rt-1636792320-6hg2v",
//    "prd-nodo-cron-job-pa-invia-rt-1636797180-8wz8v",
//    "prd-nodo-cron-job-pa-invia-rt-1636797720-hl5cj",
//    "prd-nodo-cron-job-pa-invia-rt-1636797900-l559s",
//    "prd-nodo-cron-job-pa-invia-rt-1636799520-6fkxx",
//    "prd-nodo-cron-job-pa-invia-rt-1636801740-p5vx8",
//    "prd-nodo-cron-job-pa-invia-rt-1636804020-hlbz4",
//    "prd-nodo-cron-job-pa-invia-rt-1636980420-p7xbl",
//    "prd-nodo-cron-job-pa-invia-rt-1636980900-ph96j",
//    "prd-nodo-cron-job-pa-invia-rt-1636981260-tc97q")
//  files.map(f => {
//    val content = scala.reflect.io.File(scala.reflect.io.Path.jfile2path(new java.io.File(s"C:\\env\\$f"))).slurp()
//
//    val re = "creazione actor per request paInviaRt\\[(.*)]".r
//    val matches = re.findAllMatchIn(content)
//    matches.toSeq.foreach(s => {
//      if (content.indexOf(s"Fine processo [paInviaRt][${s.group(1)}]") > 0) {
//        //println(s"${s.group(1)} finished")
//      } else {
//        println(s"\n\n${s.group(1)} not finished")
//
//        val idstaz = (s""""message":"(.*?)",.*"idStazione":"${s.group(1)}"""").r
//        val logrows = idstaz.findAllMatchIn(content)
//        logrows.map(_.group(1)).foreach(println)
//      }
//    })
//  })
//
//}
