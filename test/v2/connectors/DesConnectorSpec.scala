/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.connectors

import uk.gov.hmrc.domain.Nino
import v2.fixtures.Fixtures.DividendsFixture
import v2.mocks.{MockAppConfig, MockHttpClient}
import v2.models.Dividends
import v2.models.errors.{MultipleErrors, NinoFormatError, SingleError, TaxYearFormatError}
import v2.models.outcomes.{AmendDividendsConnectorOutcome, DesResponse}
import v2.models.requestData.{AmendDividendsRequest, DesTaxYear}

import scala.concurrent.Future

class DesConnectorSpec extends ConnectorSpec {

  trait Test extends MockHttpClient with MockAppConfig {
    val connector = new DesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )
    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  lazy val baseUrl = "test-BaseUrl"

  "calling amend" should {
    "return successful response" when {
      "valid data is supplied" in new Test {
        val nino = "AA123456A"
        val taxYear = "2018-19"
        val desTaxYear = "2019"
        val amendDividendsRequest = AmendDividendsRequest(Nino(nino), desTaxYear, DividendsFixture.dividendsModel)
        val correlationId = "X-123"
        val transactionReference = "000000000001"
        val expectedResult: DesResponse[String] = DesResponse[String](correlationId, transactionReference)

        MockedHttpClient.post[Dividends, AmendDividendsConnectorOutcome](
          s"$baseUrl" + s"/income-tax/nino/$nino/income-source/dividends/annual/$desTaxYear",
          DividendsFixture.dividendsModel)
          .returns(Future.successful(Right(expectedResult)))

        val result: AmendDividendsConnectorOutcome = await(connector.amend(amendDividendsRequest))
        result shouldBe Right(expectedResult)
      }
    }

    "return error response with correlationId and tax year format error" when {
      "the request supplied has invalid tax year" in new Test() {

        val correlationId = "X-123"
        val expectedDesResponse = DesResponse(correlationId, SingleError(TaxYearFormatError))
        val nino = "AA123456A"
        val taxYear = "2018-19"

        MockedHttpClient.post[Dividends, AmendDividendsConnectorOutcome](
          s"$baseUrl/income-tax/nino/$nino/income-source/dividends/annual/${DesTaxYear(taxYear)}",
          DividendsFixture.dividendsModel)
          .returns(Future.successful(Left(expectedDesResponse)))

        val amendDividendsRequest = AmendDividendsRequest(Nino(nino), DesTaxYear(taxYear), DividendsFixture.dividendsModel)

        val result: AmendDividendsConnectorOutcome = await(connector.amend(amendDividendsRequest))

        result shouldBe Left(expectedDesResponse)
      }
    }

    "return response with multiple errors and correlationId" when {
      "an request supplied with invalid tax year and invalid Nino " in new Test() {

        val correlationId = "X-123"
        val expectedDesResponse = DesResponse(correlationId, MultipleErrors(Seq(TaxYearFormatError, NinoFormatError)))
        val nino = "AA123456A"
        val taxYear = "2018-19"

        MockedHttpClient.post[Dividends, AmendDividendsConnectorOutcome](
          s"$baseUrl/income-tax/nino/$nino/income-source/dividends/annual/${DesTaxYear(taxYear)}",
          DividendsFixture.dividendsModel)
          .returns(Future.successful(Left(expectedDesResponse)))

        val amendDividendsRequest = AmendDividendsRequest(Nino(nino), DesTaxYear(taxYear), DividendsFixture.dividendsModel)

        val result: AmendDividendsConnectorOutcome = await(connector.amend(amendDividendsRequest))

        result shouldBe Left(expectedDesResponse)
      }
    }
  }
}