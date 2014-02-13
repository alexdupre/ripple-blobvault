package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.Done
import play.api.libs.json._
import play.api.http.HeaderNames
import play.api.data.Form
import play.api.data.Forms._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future

import models.Blob

object Vault extends Controller with MongoController {

  import play.api.Play.current
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  private def vault: JSONCollection = db.collection[JSONCollection]("vault")
  private def log: JSONCollection = db.collection[JSONCollection]("log")

  private val enableLogging = current.configuration.getBoolean("vault.log").getOrElse("false")
  private val allowOrigin = current.configuration.getString("vault.allow_origin").getOrElse("*")

  def retrieve(key: String) = Allowed {
    Action.async { implicit request =>
      vault.find(Json.obj("_id" -> key)).one[JsValue].map {
        case Some(js) =>
          val blob = (js \ "blob").as[Blob]
          Logger.debug(request.method + " " + key + " " + blob)
          logAccess(key)
          Ok(blob.toBase64)
        case None => Ok("")
      }
    }
  }

  private val blobForm = Form(single("blob" -> of[Blob]))

  def update(key: String) = Allowed {
    Action.async { implicit request =>
      blobForm.bindFromRequest.fold(
        _ => Future.successful(BadRequest),
        blob => {
          Logger.debug(request.method + " " + key + " " + blob)
          val timestamp = System.currentTimeMillis()
          val js = Json.obj("_id" -> key, "blob" -> blob, "lastaccess" -> timestamp)
          vault.save(js).map { _ =>
            logAccess(key)
            NoContent
          }
        })
    }
  }
  
  private def origin()(implicit request: RequestHeader) = request.headers.get(HeaderNames.ORIGIN)

  private def logAccess(key: String)(implicit request: Request[AnyContent]) {
    val access = Json.obj(
      "timestamp" -> System.currentTimeMillis(),
      "ip" -> request.remoteAddress,
      "origin" -> origin)
    vault.update(
      Json.obj("_id" -> key),
      Json.obj("$set" -> Json.obj("lastaccess" -> access)))
    current.configuration.getBoolean("vault.log").foreach { enabled =>
      if (enabled) {
        val js = Json.obj(
          "key" -> key,
          "action" -> request.method,
          "access" -> access)
        log.insert(js)
      }
    }
  }

  private def Allowed(action: => EssentialAction) = EssentialAction { implicit request =>
    if (allowOrigin == "*" || origin == Some(allowOrigin))
      action(request).map(_.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> allowOrigin))
    else
      Done(Forbidden.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*"))
  }

}