package au.com.seek.engineering

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import probability_monad.Distribution

import scala.concurrent.duration.{Duration, FiniteDuration}

object StencilSimulation {
  implicit def asScalaDuration(d: java.time.Duration): FiniteDuration = Duration.fromNanos(d.toNanos)

  case class Config(baseUrl: String,
                    rampUsersPerSecFrom: Int,
                    rampUsersPerSecTo: Int,
                    rampUsersPerSecDuring: FiniteDuration,
                    constantUsersPerSec: Int,
                    constantUsersPerSecDuring: FiniteDuration)

  val config = ConfigFactory.load("simulation") match {
    case c => Config(
      c.getString("base-url"),
      c.getInt("ramp-users-per-sec-from"),
      c.getInt("ramp-users-per-sec-to"),
      c.getDuration("ramp-users-per-sec-during"),
      c.getInt("constant-users-per-sec"),
      c.getDuration("constant-users-per-sec-during")
    )
  }

  /** Get the next query parameter to demonstrate using a weighted distribution */
  def nextParam: String = {
    val vs = List("value0", "value1", "value2", "value3", "value4")
    val d = Distribution.discrete(vs.indices map { x => (x, vs.length - x.toDouble) }: _*)
    vs get d.sample(1).head
  }
}

class StencilSimulation extends Simulation with LazyLogging {
  import StencilSimulation._

  logger.info(s"Running Stencil simulation against ${config.baseUrl}")

  val feeder = Iterator.continually(Map(
      "param" -> nextParam
  ))

  val workflow = feed(feeder).exec(http("GetVersion-${param}")
    .get("/version?param=${param}")
    .check(status.is(200)))
    .exec(http("GetHealth")
    .get("/health")
    .check(status.is(200)))

  val httpProtocol = http
    .baseURL(config.baseUrl)
    .inferHtmlResources()
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-GB,en-US;q=0.8,en;q=0.6")

  val users = scenario("StencilSimulation").exec(workflow)

  setUp(users.inject(
    rampUsersPerSec(config.rampUsersPerSecFrom) to config.rampUsersPerSecTo during config.rampUsersPerSecDuring,
    constantUsersPerSec(config.constantUsersPerSec) during config.constantUsersPerSecDuring)
  ).protocols(httpProtocol)
}
