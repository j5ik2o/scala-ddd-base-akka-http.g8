package $package$.interface.repository

import $package$.domain.model.{ UserAccount, UserAccountId }
import _root_.slick.jdbc.JdbcProfile
import com.github.j5ik2o.dddbase._
import monix.eval.Task

trait UserAccountRepository[M[_]]
    extends AggregateSingleReader[M]
    with AggregateSingleWriter[M]
    with AggregateMultiReader[M]
    with AggregateMultiWriter[M]
    with AggregateSingleSoftDeletable[M]
    with AggregateMultiSoftDeletable[M] {
  override type IdType        = UserAccountId
  override type AggregateType = UserAccount
}

object UserAccountRepository {

  type BySlick[A] = Task[A]

  def bySlick(profile: JdbcProfile, db: JdbcProfile#Backend#Database): UserAccountRepository[BySlick] =
    new UserAccountRepositoryOnJDBC(profile, db)

}
