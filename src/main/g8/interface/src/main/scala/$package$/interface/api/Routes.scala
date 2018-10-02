package $package$.interface.api

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.{ Directives, Route, StandardRoute }
import wvlet.airframe._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

trait Routes extends Directives {

  // private lazy val itemController    = bind[ItemController]
  private lazy val swaggerDocService = bind[SwaggerDocService]

  private def index(): StandardRoute = complete(
    HttpResponse(
      entity = HttpEntity(
        ContentTypes.`text/plain(UTF-8)`,
        "Wellcome to API"
      )
    )
  )

  def routes: Route = cors() {
    pathEndOrSingleSlash {
      index()
    } ~ path("swagger") {
      getFromResource("swagger/index.html")
    } ~ getFromResourceDirectory("swagger") ~
    swaggerDocService.routes // ~ itemController.route
  }

}
