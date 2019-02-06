package com.nd.validationservice

import io.circe.generic.auto._
import io.finch.circe._
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Await
import io.finch._
import Endpoints._

object Main extends App {

  def service: Service[Request, Response] = Bootstrap
    .serve[Application.Json](testLciReport)
    .toService

  Await.ready(com.twitter.finagle.Http.server.serve(":8083", service))
}