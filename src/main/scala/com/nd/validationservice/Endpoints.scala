package com.nd.validationservice

import cats.effect.IO
import io.finch._
import io.finch.circe._
import io.finch.catsEffect._
import io.circe._
import io.circe.generic.semiauto.deriveEncoder
import io.circe.generic.auto._
import io.circe.parser._
import decorators._
import scalaj.http._
import com.typesafe.config.ConfigFactory
import org.scalatest.events.Event
import org.scalatest.{Args, Reporter}

import scala.language.postfixOps

object Endpoints {

  val rootUrl = "api" :: "v1"

  val schedulerUrl: String = ConfigFactory.load().getString("validation-service.scheduler-mock-service-url") + "report/lci"
  val resultsServiceUrl: String = ConfigFactory.load().getString("validation-service.results-mock-service-url")
  val statusScheduled: String = ConfigFactory.load().getString("validation-service.statuses.scheduled")
  val statusFinished: String = ConfigFactory.load().getString("validation-service.statuses.finished")
  val statusFailed: String = ConfigFactory.load().getString("validation-service.statuses.failed")
  val testStatusPassed: String =  ConfigFactory.load().getString("validation-service.test-statuses.passed")
  val testStatusFailed: String =  ConfigFactory.load().getString("validation-service.test-statuses.failed")
  val invalidSchedulerResponse: String = ConfigFactory.load().getString("validation-service.test-messages.invalid-scheduler-response")
  val invalidResultsResponse: String = ConfigFactory.load().getString("validation-service.test-messages.invalid-results-response")
  val reportStillRunning: String = ConfigFactory.load().getString("validation-service.test-messages.report-still-running")
  val successfullyApplied: String = ConfigFactory.load().getString("validation-service.test-messages.successfully-applied")
  val reportStatusFinished: String = ConfigFactory.load().getString("validation-service.report-statuses.finished")

  implicit val decodeTest: Encoder[Test] = deriveEncoder[Test]

  def testLciReport: Endpoint[IO, TestResults] = post(rootUrl :: "report" :: "lci" :: "test" :: jsonBody[LciTestRequestBody]) {
    lciTestRequestBody: LciTestRequestBody =>
      withInternalServerError[TestResults] {
        val resultsFromSchedulerService = getResultsFromSchedulerService(lciTestRequestBody)
        resultsFromSchedulerService match {
          case Left(res) => Ok(TestResults(statusFailed, List[Test](), invalidSchedulerResponse))
          case Right(res) =>
            if (res.status != reportStatusFinished) {
              Ok(TestResults(statusScheduled, List[Test](), reportStillRunning + ", jobId " + res.jobId))
            }
            else {
              val resultsFromRsultsService = getResultsFromResultsService(res.jobId)
              resultsFromRsultsService match {
                case Left(res) => Ok(TestResults(statusFailed, List[Test](), invalidResultsResponse))
                case Right(res) => Ok(runTests(res, lciTestRequestBody.shouldBeEqualTo))
              }
            }
        }
      }
  }

  def getResultsFromSchedulerService(lciTestRequestBody: LciTestRequestBody): Either[io.circe.Error, Job] = {
    val jobName: String = lciTestRequestBody.lciRequestBody.jobName
    val campaignId: Int = lciTestRequestBody.lciRequestBody.campaignId
    val reportDate: String = lciTestRequestBody.lciRequestBody.reportDate
    try {
      val response: HttpResponse[String] = Http(schedulerUrl).postData(
        s"""
           |{
           |	"jobName": "$jobName",
           |	"campaignId": $campaignId,
           |	"reportDate": "$reportDate"
           |}
          """.stripMargin
      )
        .header("Content-Type", "application/json")
        .option(HttpOptions.readTimeout(10000))
        .asString
      decode[Job](response.body)
    }
    catch {
      case e: Throwable => Left(ParsingFailure("", new Exception))
    }
  }

  def getResultsFromResultsService(jobId: String): Either[io.circe.Error, LciReportResults] = {
    val url = resultsServiceUrl + "job/" + jobId
    try {
      val response: HttpResponse[String] = Http(url).asString
      decode[LciReportResults](response.body)
    }
    catch {
      case e: Throwable => Left(ParsingFailure("", new Exception))
    }
  }

  def runTests(results: LciReportResults, shouldBe: LciReportResults): TestResults = {
    val reporter = new Reporter() {
      override def apply(e: Event) = {}
    }
    val testSuite = new Tests(results, shouldBe)
    val testNames = testSuite.testNames
    val tests: Set[Test] = for(test: String <- testNames) yield {
      val result = testSuite.run(Some(test), Args(reporter))
      val status = if (result.succeeds()) testStatusPassed else testStatusFailed
      Test(test, status)
    }
    TestResults(statusFinished, tests.toList, successfullyApplied)
  }
}
