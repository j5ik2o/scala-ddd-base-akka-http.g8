package $package$.domain.model

import org.sisioh.baseunits.scala.time.TimePoint
import com.github.j5ik2o.dddbase.{ Aggregate, AggregateLongId }

import scala.reflect._

case class UserAccountId(value: Long) extends AggregateLongId

case class EmailAddress(value: String)

case class HashedPassword(value: String)

case class UserAccount(id: UserAccountId,
                       status: Status,
                       emailAddress: EmailAddress,
                       password: HashedPassword,
                       firstName: String,
                       lastName: String,
                       createdAt: TimePoint,
                       updatedAt: Option[TimePoint])
    extends Aggregate {
  override type AggregateType = UserAccount
  override type IdType        = UserAccountId
  override protected val tag: ClassTag[UserAccount] = classTag[UserAccount]
}
