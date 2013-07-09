package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.FormError
import play.api.data.format.Formatter
import scala.util.control.Exception
import java.net.{URLDecoder,URLEncoder}

import models.Implicits._

case class Blob(iv: Array[Byte], v: Int, iter: Int, ks: Int, ts: Int, mode: String, adata: String, cipher: String, salt: Array[Byte], ct: Array[Byte]) {
  
  def toBase64(): String =
    Json.toJson(this).toString.getBytes("UTF-8").toB64
  
  override def toString = s"Blob(iv(${iv.toB64}),v($v),iter($iter),ks($ks),ts($ts),mode($mode),adata($adata),cipher($cipher),salt(${salt.toB64}),ct(${ct.toB64}))" 
}

object Blob {

  private val base64Format = new Format[Array[Byte]] {
    def reads(json: JsValue): JsResult[Array[Byte]] =
      json.asOpt[String].map(_.fromB64) match {
        case Some(data) if data.size > 0 => JsSuccess(data)
        case _ => JsError()
      }

    def writes(o: Array[Byte]): JsValue =
      new JsString(o.toB64)
  }

  private val urlFormat = new Format[String] {
    def reads(json: JsValue): JsResult[String] =
      json.asOpt[String].map(URLDecoder.decode(_, "ASCII")) match {
        case Some(data) if data.size > 0 => JsSuccess(data)
        case _ => JsError()
      }

    def writes(o: String): JsValue =
      new JsString(URLEncoder.encode(o, "ASCII"))
  }

  implicit val blobFormat: Format[Blob] = (
    (__ \ "iv").format(base64Format) and
    (__ \ "v").format[Int] and
    (__ \ "iter").format[Int] and
    (__ \ "ks").format[Int] and
    (__ \ "ts").format[Int] and
    (__ \ "mode").format[String] and
    (__ \ "adata").format(urlFormat) and
    (__ \ "cipher").format[String] and
    (__ \ "salt").format(base64Format) and
    (__ \ "ct").format(base64Format))(Blob.apply, unlift(Blob.unapply))

  implicit val blobFormatter = new Formatter[Blob] {
    def bind(key: String, data: Map[String, String]) = {
      data.get(key).toRight {
        Seq(FormError(key, "error.required", Nil))
      }.right.flatMap { string =>
        Exception.nonFatalCatch[Blob]
          .either(Blob.fromBase64(string))
          .left.map { exception =>
            Seq(FormError(key, "error.invalid", Nil))
          }
      }
    }
    def unbind(key: String, blob: Blob) = Map(key -> blob.toBase64)
  }

  def fromBase64(b64: String): Blob =
    Json.parse(b64.fromB64).as[Blob]
}
