package gatlingdemostore

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class DemostoreSimulation extends Simulation {

	val domain = "demostore.gatling.io"

	val httpProtocol = http
		.baseUrl("http://" + domain)

	val scn = scenario("RecordedSimulation")
		.exec(http("Load Home Page")
			.get("/")
			.check(regex("<title>Gatling Demo-Store</title>").exists)
			.check(css("#_csrf", "content").saveAs("csrfValue")))
		.pause(2)
		.exec(http("Load About Us Page")
			.get("/about-us"))
		.pause(2)
		.exec(http("Load Categories Page")
			.get("/category/all"))
		.pause(2)
		.exec(http("Load Product Page")
			.get("/product/black-and-red-glasses"))
		.pause(2)
		.exec(http("Add Product to Cart")
			.get("/cart/add/19"))
		.pause(2)
		.exec(http("View Cart")
			.get("/cart/view"))
		.pause(2)
		.exec(http("Login User")
			.post("/login")
			.formParam("_csrf", "${csrfValue}")
			.formParam("username", "user1")
			.formParam("password", "pass"))
		.pause(2)
		.exec(http("Checkout")
			.get("/cart/checkout"))

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
