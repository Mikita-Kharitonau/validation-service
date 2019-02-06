package com.nd.validationservice

import org.scalatest.FlatSpec

class Tests(results: LciReportResults, shouldBe: LciReportResults) extends FlatSpec {

  "results" should "be equal to given json" in {
    assert(results == shouldBe)
  }

}
