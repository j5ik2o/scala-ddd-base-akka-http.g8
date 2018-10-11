package $package$.useCase

import akka.NotUsed
import akka.stream.scaladsl._
import monix.eval.Task
import monix.execution.Scheduler
import org.sisioh.baseunits.scala.timeutil.Clock
import $package$.domain.model._
import $package$.useCase.model.{ CreateUserAccountRequest, CreateUserAccountResponse }
import $package$.useCase.port.generator.IdGenerator
import $package$.useCase.port.repository.UserAccountRepository
import wvlet.airframe._

trait UserAccountUseCase {

  private val userIdGenerator = bind[IdGenerator[UserAccountId]]
  private val userRepository  = bind[UserAccountRepository[Task]]

  def create(implicit scheduler: Scheduler): Flow[CreateUserAccountRequest, CreateUserAccountResponse, NotUsed] =
    Flow[CreateUserAccountRequest].mapAsync(1) { userAccount =>
      (for {
        id <- userIdGenerator.generateId()
        result <- userRepository.store(
          UserAccount(
            id,
            Status.Active,
            EmailAddress(userAccount.emailAddress),
            HashedPassword(userAccount.password),
            userAccount.firstName,
            userAccount.lastName,
            Clock.now,
            None
          )
        )
      } yield CreateUserAccountResponse(id.value)).runAsync
    }

  def resolveById(id: UserAccountId)(implicit scheduler: Scheduler): Source[UserAccount, NotUsed] =
    Source.fromFuture {
      userRepository.resolveById(id).runAsync
    }

}
