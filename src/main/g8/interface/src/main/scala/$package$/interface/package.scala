package $package$

import monix.eval.Task
import $package$.domain.model.UserAccountId
import $package$.interface.api.controller.UserAccountController
import $package$.interface.api.{ApiServer, Routes, SwaggerDocService}
import $package$.interface.generator.{IdGenerator, UserAccountIdGeneratorOnJDBC}
import $package$.interface.repository.{UserAccountRepository, UserAccountRepositoryOnJDBC}
import wvlet.airframe._

package object interface {

  def createInterfaceDesign(host: String, port: Int, apiClasses: Set[Class[_]]): Design =
    newDesign
      .bind[Routes].toSingleton
      .bind[SwaggerDocService].toInstance(new SwaggerDocService(host, port, apiClasses))
      .bind[ApiServer].toSingleton
      .bind[UserAccountRepository[Task]].to[UserAccountRepositoryOnJDBC]
      .bind[IdGenerator[UserAccountId]].to[UserAccountIdGeneratorOnJDBC]
      .bind[UserAccountController].toSingleton

}
