package $package$

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ServerSettings
import $package$.interface.api.ApiServer
import $package$.interface.api.controller.UserAccountController
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
    val salt = system.settings.config.getString("$package$.interface.hashids.salt")

    parser.parse(args, AppConfig()) match {
      case Some(config) =>
        val design = newDesign
          .bind[ActorSystem].toInstance(system)
          .bind[JdbcProfile].toInstance(dbConfig.profile)
          .bind[JdbcProfile#Backend#Database].toInstance(dbConfig.db)
          .add(interface.createInterfaceDesign(config.host, config.port, salt, Set(classOf[UserAccountController])))

        design.withSession { session =>
          session.build[ApiServer].start(config.host, config.port, settings = ServerSettings(system))
        }
      case None =>
        println(parser.usage)
    }
  }
}