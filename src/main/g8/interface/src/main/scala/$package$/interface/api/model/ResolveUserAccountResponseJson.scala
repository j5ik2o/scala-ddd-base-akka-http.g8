package sample.interface.api.model

case class ResolveUserAccountResponseJson(override val body: Either[ErrorResponseBody, ResolveUserAccountResponseBody])
    extends BaseResponse[ResolveUserAccountResponseBody]
