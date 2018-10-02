package $package$.interface.api.model

case class CreateUserAccountResponseJson(override val body: Either[ErrorResponseBody, CreateUserAccountResponseBody])
  extends BaseResponse[CreateUserAccountResponseBody]


