package gatlingdemostore

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class DemostoreSimulation extends Simulation {

	val httpProtocol = http
		.baseUrl("https://demostore-gatling.io")
		.inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.woff2""", """.*\.(t|o)tf""", """.*\.png""", """.*detectportal\.firefox\.com.*"""), WhiteList())
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("en-GB,en-US;q=0.9,en;q=0.8")
		.userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36")

	val headers_0 = Map(
		"Sec-Fetch-Dest" -> "document",
		"Sec-Fetch-Mode" -> "navigate",
		"Sec-Fetch-Site" -> "same-origin",
		"Sec-Fetch-User" -> "?1",
		"Upgrade-Insecure-Requests" -> "1")

	val headers_4 = Map(
		"Accept" -> "*/*",
		"Sec-Fetch-Dest" -> "empty",
		"Sec-Fetch-Mode" -> "cors",
		"Sec-Fetch-Site" -> "same-origin",
		"X-Requested-With" -> "XMLHttpRequest")

	val headers_6 = Map(
		"Cache-Control" -> "max-age=0",
		"Origin" -> "https://gatling-demostore.com",
		"Sec-Fetch-Dest" -> "document",
		"Sec-Fetch-Mode" -> "navigate",
		"Sec-Fetch-Site" -> "same-origin",
		"Sec-Fetch-User" -> "?1",
		"Upgrade-Insecure-Requests" -> "1")



	val scn = scenario("RecordedSimulation")
		.exec(http("request_0")
			.get("/")
			.headers(headers_0))
		.pause(3)
		.exec(http("request_1")
			.get("/about-us")
			.headers(headers_0))
		.pause(4)
		.exec(http("request_2")
			.get("/category/all")
			.headers(headers_0))
		.pause(5)
		.exec(http("request_3")
			.get("/product/black-and-red-glasses")
			.headers(headers_0))
		.pause(5)
		.exec(http("request_4")
			.get("/cart/add/19")
			.headers(headers_4))
		.pause(3)
		.exec(http("request_5")
			.get("/cart/view")
			.headers(headers_0))
		.pause(8)
		.exec(http("request_6")
			.post("/login")
			.headers(headers_6)
			.formParam("_csrf", "497b3e3d-0067-445c-8452-98e9fabe037c")
			.formParam("username", "user1")
			.formParam("password", "password1"))
		.pause(7)
		.exec(http("request_7")
			.get("/cart/checkout")
			.headers(headers_0))

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}