package gatlingdemostore

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import scala.util.Random

class DemostoreSimulation extends Simulation {

	val domain = "demostore.gatling.io"

	val httpProtocol = http
		.baseUrl("https://" + domain)

	def userCount: Int = getProperty("USERS", "5").toInt
	def rampDuration: Int = getProperty("RAMP_DURATION", "10").toInt
	def testDuration: Int = getProperty("DURATION", "60").toInt

	private def getProperty(propertyName: String, defaultValue: String) = {
		Option(System.getenv(propertyName))
			.orElse(Option(System.getProperty(propertyName)))
			.getOrElse(defaultValue)
	}

	val categoryFeeder = csv("data/categoryDetails.csv").random
	val jsonFeederProducts = jsonFile("data/productDetails.json").random
	val csvFeederLoginDetails = csv("data/loginDetails.csv").circular

	val rnd = new Random()

	def randomString(length: Int): String = {
		rnd.alphanumeric.filter(_.isLetter).take(length).mkString
	}

	val initSession = exec(flushCookieJar)
		.exec(session => session.set("randomNumber", rnd.nextInt))
		.exec(session => session.set("customerLoggedIn", false))
		.exec(session => session.set("cartTotal", 0.00))
		.exec(addCookie(Cookie("sessionId", randomString(10)).withDomain(domain)))

	object CmsPages {
		def homepage = {
			exec(http("Load Home Page")
				.get("/")
				.check(status.is(200))
				.check(regex("<title>Gatling Demo-Store</title>").exists)
				.check(css("#_csrf", "content").saveAs("csrfValue")))
		}

		def aboutUs = {
			exec(http("Load About Us Page")
				.get("/about-us")
				.check(status.is(200))
				.check(substring("About Us"))
			)
		}
	}

	object Catalog {
		object Category {
			def view = {
				feed(categoryFeeder)
					.exec(http("Load Category Page - ${categoryName}")
						.get("/category/${categorySlug}")
						.check(status.is(200))
						.check(css("#CategoryName").is("${categoryName}"))
					)
			}
		}

		object Product {
			def view = {
				feed(jsonFeederProducts)
					.exec(
						http("Load Product Page - ${name}")
							.get("/product/${slug}")
							.check(status.is(200))
							.check(css("#ProductDescription").is("${description}"))
					)
			}

			def add = {
				exec(view)
				.exec(
					http("Add product to cart")
						.get("/cart/add/${id}")
						.check(status.is(200))
						.check(substring("items in your cart"))
				)
					.exec(session => {
						val currentCartTotal = session("cartTotal").as[Double]
						val itemPrice = session("price").as[Double]
						session.set("cartTotal", (currentCartTotal + itemPrice))
					})
			}
		}
	}

	object Customer {
		def login = {
			feed(csvFeederLoginDetails)
				.exec(
					http("Load Login Page")
						.get("/login")
						.check(status.is(200))
						.check(substring("Username:"))
				)
				.exec(
					http("Customer Login Action")
						.post("/login")
						.formParam("_csrf", "${csrfValue}")
						.formParam("username", "${username}")
						.formParam("password", "${password}")
						.check(status.is(200))
				)
				.exec(session => session.set("customerLoggedIn", true))
		}
	}

	object Checkout {
		def viewCart = {
			doIf(session => !session("customerLoggedIn").as[Boolean]) {
				exec(Customer.login)
			}
			.exec(
				http("Load Cart Page")
					.get("/cart/view")
					.check(status.is(200))
					.check(css("#grandTotal").is("$$${cartTotal}"))
			)
		}

		def completeCheckout = {
			exec(
				http("Checkout Cart")
					.get("/cart/checkout")
					.check(status.is(200))
					.check(substring("Thanks for your order! See you soon!"))
			)
		}
	}

	val scn = scenario("DemostoreSimulation")
		.exec(initSession)
		.exec(CmsPages.homepage)
		.pause(2)
		.exec(CmsPages.aboutUs)
		.pause(2)
		.exec(Catalog.Category.view)
		.pause(2)
		.exec(Catalog.Product.add)
		.pause(2)
		.exec(Checkout.viewCart)
		.pause(2)
		.exec(Checkout.completeCheckout)

	object UserJourneys {
		def minPause = 100.milliseconds
		def maxPause = 500.milliseconds

		def browseStore = {
			exec(initSession)
			.exec(CmsPages.homepage)
				.pause(maxPause)
				.exec(CmsPages.aboutUs)
				.pause(minPause, maxPause)
				.repeat(5) {
					exec(Catalog.Category.view)
						.pause(minPause, maxPause)
						.exec(Catalog.Product.view)
				}
		}

		def abandonCart = {
			exec(initSession)
				.exec(CmsPages.homepage)
				.pause(maxPause)
				.exec(Catalog.Category.view)
				.pause(minPause, maxPause)
				.exec(Catalog.Product.view)
				.pause(minPause, maxPause)
				.exec(Catalog.Product.add)
		}

		def completePurchase = {
			exec(initSession)
				.exec(CmsPages.homepage)
				.pause(maxPause)
				.exec(Catalog.Category.view)
				.pause(minPause, maxPause)
				.exec(Catalog.Product.view)
				.pause(minPause, maxPause)
				.exec(Catalog.Product.add)
				.pause(minPause, maxPause)
				.exec(Checkout.viewCart)
				.pause(minPause, maxPause)
				.exec(Checkout.completeCheckout)
		}
	}

	object Scenarios {
		def default = scenario("Default Load Test")
			.during(60.seconds) {
				randomSwitch(
					75d -> exec(UserJourneys.browseStore),
					15d -> exec(UserJourneys.abandonCart),
					10d -> exec(UserJourneys.completePurchase)
				)
			}

		def highPurchase = scenario("High Purhcase Load Test")
			.during(60.seconds) {
				randomSwitch(
					25d -> exec(UserJourneys.browseStore),
					25d -> exec(UserJourneys.abandonCart),
					50d -> exec(UserJourneys.completePurchase)
				)
			}
	}
	
	setUp(scn.inject(constantUsersPerSec(1) during (3.minutes))).protocols(httpProtocol).throttle(
		reachRps(10) in (30.seconds),
		holdFor(60.seconds),
		jumpToRps(20),
		holdFor(60.seconds)
	).maxDuration(3.minutes)

}