package gatlingdemostore

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class DemostoreSimulation extends Simulation {

	val domain = "demostore.gatling.io"

	val httpProtocol = http
		.baseUrl("https://" + domain)

	val categoryFeeder = csv("data/categoryDetails.csv").random
	val jsonFeederProducts = jsonFile("data/productDetails.json").random
	val csvFeederLoginDetails = csv("data/loginDetails.csv").circular

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
					.exec(http("Load Product Page - ${name}")
						.get("/product/${slug}")
						.check(status.is(200))
						.check(css("#ProductDescription").is("${description}"))
					)
			}

			def add = {
				exec(view).
					exec(http("Add Product to Cart")
						.get("/cart/add/${id}")
						.check(status.is(200))
						.check(substring("items in your cart"))
					)
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
		}
	}

	object Checkout {
		def viewCart = {
			exec(
				http("Load Cart Page")
					.get("/cart/view")
					.check(status.is(200))
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

	val scn = scenario("RecordedSimulation")
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
		.exec(Customer.login)
		.pause(2)
		.exec(Checkout.completeCheckout)

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}