package $package$

import monix.eval.Task
import org.hashids.Hashids
import $package$.domain.model.UserAccountId
import $package$.interface.api.controller.UserAccountController
import $package$.interface.api.{ApiServer, Routes, SwaggerDocService}
import $package$.interface.generator.{IdGenerator, UserAccountIdGeneratorOnJDBC}
import $package$.interface.repository.UserAccountRepositoryOnJDBC
import $package$.useCase.port.repository.UserAccountRepository
import wvlet.airframe._

package object interface {

  def createInterfaceDesign(host: String, port: Int, hashidsSalt: String, apiClasses: Set[Class[_]]): Design =
    newDesign
      .bind[Hashids].toInstance(new Hashids(hashidsSalt))
      .bind[Routes].toSingleton
      .bind[SwaggerDocService].toInstance(new SwaggerDocService(host, port, apiClasses))
      .bind[ApiServer].toSingleton
      .bind[UserAccountRepository[Task]].to[UserAccountRepositoryOnJDBC]
      .bind[IdGenerator[UserAccountId]].to[UserAccountIdGeneratorOnJDBC]
      .bind[UserAccountController].toSingleton

}
