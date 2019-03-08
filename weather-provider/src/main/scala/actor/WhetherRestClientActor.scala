package actor

import java.util.Properties

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import ua.ucu.edu.Main.system
import ua.ucu.edu.model.Location

import scala.concurrent.Future

object Config {
  val KafkaBrokers = "KAFKA_BROKERS"
  val WeatherTopic = "WEATHER_TOPIC_NAME"
}

class WhetherRestClientActor extends Actor with ActorLogging {
  import WhetherRestClientActor._
  import system.dispatcher
  import scala.util.{Failure, Success}
  implicit val actorSystem: ActorSystem = context.system

//  val BrokerList: String = System.getenv(Config.KafkaBrokers)
//  val topic = System.getenv(Config.WeatherTopic)
//  val props = new Properties()
//
//  log.info("[Kafka] Started topic: {}", topic)
//
//  props.put("bootstrap.servers", BrokerList)
//  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
//  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

//  val producer = new KafkaProducer[String, String](props)

  val API_URL = System.getenv("WEATHER_API_URL")
  val API_KEY = System.getenv("WEATHER_API_KEY")

  var i = 0

  override def receive: Receive = {
    case InitiateLocationRequest => {
      val loc = Locations.getByIndex(i)
      weatherAtLocation(loc.latitude.toString, loc.longitude.toString)
        .map(x => {
          i += 1
          x
        })
        .onComplete {
          case Success(s) => {
            log.info("[Kafka] Stream data\n: {}", s._3.toString)
//            val data = new ProducerRecord[String, String](topic, s._3.toString)
//            producer.send(data)
          }
          case Failure(f) => {
            println(f.getMessage)
          }
      }
    }
    case InitiateCityRequest => {
      val loc = Locations.getByIndex(i)
      weatherAtCity(loc.city)
        .map(x => {
          i += 1
          x
        })
        .onComplete {
          case Success(s) => {
            log.info("[Kafka] Stream data\n: {}", s._3.toString)
//            val data = new ProducerRecord[String, String](topic, s._3.toString)
//            producer.send(data)
          }
          case Failure(f) => {
            println(f.getMessage)
          }
      }
    }
  }

  def weatherAtLocation(lon: String, lat: String): Future[HttpResponse] = {
    val url = Uri(API_URL)
    Http().singleRequest(
      HttpRequest(
        HttpMethods.GET,
        url.withQuery(Uri.Query(
          ("lon" -> lon),
          ("lat" -> lat),
          ("APPID" -> API_KEY),
        ))
      )
    )
  }

  def weatherAtCity(city: String): Future[HttpResponse] = {
    val url = Uri(API_URL)
    Http().singleRequest(
      HttpRequest(
        HttpMethods.GET,
        url.withQuery(Uri.Query(
          ("q" -> city),
          ("APPID" -> API_KEY),
        ))
      )
    )
  }

  override def postStop(): Unit = {
    log.info("[Stopped] weather data producer")
//    producer.close()
  }
}

object WhetherRestClientActor {
  case object InitiateLocationRequest
  case object InitiateCityRequest
  case object Locations {
    private var locations: List[Location] = List[Location]()
    def set(loc: List[Location]): Unit = locations = loc
    def get(): List[Location] = locations
    def getByIndex(i: Int): Location = {
      locations(i)
    }
  }
}
