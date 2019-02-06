package com.nd.validationservice

import io.finch._


object decorators {
  def withInternalServerError[T](func: => Output[T]): Output[T] = {
    try {
      func
    }
    catch {
      case e: Throwable =>
        e.printStackTrace
        InternalServerError(new Exception("Something went wrong on server:("))
    }
  }
}
