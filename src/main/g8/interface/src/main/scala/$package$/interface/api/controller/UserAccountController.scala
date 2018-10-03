package $package$.interface.api.controller

import akka.http.scaladsl.server.{Directives, Route}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import javax.ws.rs._
import monix.eval.Task
import monix.execution.Scheduler
import org.hashids.Hashids
import org.sisioh.baseunits.scala.timeutil.Clock
import $package$.domain.model._
import $package$.interface.api.model.{CreateUserAccountRequestJson, CreateUserAccountResponseBody, CreateUserAccountResponseJson}
import $package$.interface.generator.IdGenerator
import $package$.useCase.port.repository.UserAccountRepository
import wvlet.airframe._

import scala.concurrent.Future

@Path("/user_accounts")
@Consumes(Array("application/json"))
@Produces(Array("application/json"))
trait UserAccountController extends Directives {

  private val userIdGenerator = bind[IdGenerator[UserAccountId]]
  private val userRepository = bind[UserAccountRepository[Task]]

  private val hashids = bind[Hashids]

  private def convertToAggregate(id: UserAccountId, request: CreateUserAccountRequestJson): UserAccount = UserAccount(
    id = id,
    status = Status.Active,
    emailAddress = EmailAddress(request.emailAddress),
    password = HashedPassword(request.password),
    firstName = request.firstName,
    lastName = request.lastName ,
    createdAt = Clock.now,
    updatedAt = None
  )

  @POST
  @Operation(
    summary = "Create UserAccount",
    description = "Create UserAccount",
    requestBody =
      new RequestBody(content = Array(new Content(schema = new Schema(implementation = classOf[CreateUserAccountRequestJson])))),
    responses = Array(
      new ApiResponse(responseCode = "200",
        description = "Create response",
        content = Array(new Content(schema = new Schema(implementation = classOf[CreateUserAccountResponseJson])))),
      new ApiResponse(responseCode = "400", description = "Bad request"),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  def create: Route = path("user_accounts") {
    post {
      extractActorSystem { implicit system =>
        implicit val scheduler: Scheduler = Scheduler(system.dispatcher)
        entity(as[CreateUserAccountRequestJson]) { request =>
          val future: Future[CreateUserAccountResponseJson] = (for {
            userAccountId <- userIdGenerator.generateId()
            _      <- userRepository.store(convertToAggregate(userAccountId, request))
          } yield CreateUserAccountResponseJson(Right(CreateUserAccountResponseBody(hashids.encode(userAccountId.value))))).runAsync
          onSuccess(future) { result =>
            complete(result)
          }
        }
      }
    }
  }

}
