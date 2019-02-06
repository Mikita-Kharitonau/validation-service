package com.nd.validationservice

case class LciRequestBody(
  jobName: String,
  campaignId: Int,
  reportDate: String
)

case class LciReportResults(
  campaignId: Int,
  reportDate: String,
  numberOfVisitors: Option[Int],
  numberOfImpressions: Option[Int]
)

case class LciTestRequestBody(
  lciRequestBody: LciRequestBody,
  shouldBeEqualTo: LciReportResults
)

case class Job(
  jobId: String,
  status: String
)

case class TestResults(
  status: String,
  tests: List[Test],
  message: String
)

case class Test(
  name: String,
  status: String
)

