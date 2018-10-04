package $package$

import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.scaladsl.{Sink, Source}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import javax.ws.rs._
import monix.execution.Scheduler
import org.hashids.Hashids
import $package$.interface.api.model.{CreateUserAccountRequestJson, CreateUserAccountResponseBody, CreateUserAccountResponseJson}
import $package$.useCase.UserAccountUseCase
import $package$.useCase.model.CreateUserAccountRequest
import wvlet.airframe._

import scala.concurrent.Future

@Path("/user_accounts")
@Consumes(Array("application/json"))
@Produces(Array("application/json"))
trait UserAccountController extends Directives {

  private val userAccountUseCase = bind[UserAccountUseCase]

  private val hashids = bind[Hashids]

  def route: Route = create

  def extractScheduler: Directive1[Scheduler] = extractActorSystem.tmap { s =>
    Scheduler(s._1.dispatcher)
  }

  @POST
  @Operation(
    summary = "Create UserAccount",
    description = "Create UserAccount",
    requestBody = new RequestBody(
      content = Array(new Content(schema = new Schema(implementation = classOf[CreateUserAccountRequestJson])))
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Create response",
        content = Array(new Content(schema = new Schema(implementation = classOf[CreateUserAccountResponseJson])))
      ),
      new ApiResponse(responseCode = "400", description = "Bad request"),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  def create: Route = path("user_accounts") {
    post {
      extractMaterializer { implicit mat =>
        extractScheduler { implicit scheduler =>
          entity(as[CreateUserAccountRequestJson]) { request =>
            val future: Future[CreateUserAccountResponseJson] = Source
              .single(request).map { request =>
              CreateUserAccountRequest(request.emailAddress, request.password, request.firstName, request.lastName)
            }.via(userAccountUseCase.store).map { response =>
              CreateUserAccountResponseJson(Right(CreateUserAccountResponseBody(hashids.encode(response.id))))
            }.runWith(Sink.head)
            onSuccess(future) { result =>
              complete(result)
            }
          }
        }
      }
    }
  }

}
