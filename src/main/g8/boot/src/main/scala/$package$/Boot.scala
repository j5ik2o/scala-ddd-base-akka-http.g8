package $package$

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ServerSettings
import org.hashids.Hashids
import $package$.interface.api.ApiServer
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import wvlet.airframe._

object Boot {

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[AppConfig]("$package$") {
      opt[String]('h', "host").action((x, c) => c.copy(host = x)).text("host")
      opt[Int]('p', "port").action((x, c) => c.copy(port = x)).text("port")
    }
    val system = ActorSystem("$package$")
    val dbConfig: DatabaseConfig[JdbcProfile] =
      DatabaseConfig.forConfig[JdbcProfile](path = "$package$.interface.storage.jdbc", system.settings.config)

    parser.parse(args, AppConfig()) match {
      case Some(config) =>
        val design = newDesign
          .bind[Hashids].toInstance(new Hashids(system.settings.config.getString("$package$.interface.hashids.salt")))
          .bind[ActorSystem].toInstance(system)
          .bind[JdbcProfile].toInstance(dbConfig.profile)
          .bind[JdbcProfile#Backend#Database].toInstance(dbConfig.db)

        design.withSession { session =>
          val system = session.build[ActorSystem]
          session.build[ApiServer].start(config.host, config.port, settings = ServerSettings(system))
        }
      case None =>
        println(parser.usage)
    }
  }
}