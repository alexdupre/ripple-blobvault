package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.http.HeaderNames
import play.api.data.Form
import play.api.data.Forms._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

import models.Blob

object Vault extends Controller with MongoController {

  import play.api.Play.current

  private def vault: JSONCollection = db.collection[JSONCollection]("vault")
  private def log: JSONCollection = db.collection[JSONCollection]("log")

  private val enableLogging = current.configuration.getBoolean("vault.log").getOrElse("false")
  private val allowOrigin = current.configuration.getString("vault.allow_origin").getOrElse("*")

  def retrieve(key: String) = AllowedAction { implicit request =>
    Async {
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

  def update(key: String) = AllowedAction { implicit request =>
    blobForm.bindFromRequest.fold(
      _ => BadRequest,
      blob => {
        Async {
          Logger.debug(request.method + " " + key + " " + blob)
          val timestamp = System.currentTimeMillis()
          val js = Json.obj("_id" -> key, "blob" -> blob, "lastaccess" -> timestamp)
          vault.save(js).map { _ =>
            logAccess(key)
            NoContent
          }
        }
      })
  }
  
  private def getOrigin()(implicit request: Request[AnyContent]) = request.headers.get(HeaderNames.ORIGIN)

  private def logAccess(key: String)(implicit request: Request[AnyContent]) {
    val access = Json.obj(
      "timestamp" -> System.currentTimeMillis(),
      "ip" -> request.remoteAddress,
      "origin" -> getOrigin)
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

  private def AllowedAction(f: Request[AnyContent] => Result): Action[AnyContent] = {
    Action { implicit request =>
      if (allowOrigin == "*" || getOrigin == Some(allowOrigin))
        f(request).withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> allowOrigin)
      else
        Forbidden.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*")
    }
  }

}