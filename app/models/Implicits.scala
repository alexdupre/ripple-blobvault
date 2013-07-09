package models

import org.apache.commons.codec.binary.Base64

object Implicits {

  implicit def toEncodable(bytes: Array[Byte]) = new AnyRef {
    def toB64() = new String(Base64.encodeBase64(bytes), "ASCII")
  }

  implicit def toDecodable(str: String) = new AnyRef {
    def fromB64() = Base64.decodeBase64(str.getBytes("ASCII"))
  }

}