package models

import org.apache.commons.codec.binary.Base64

object Implicits {

  implicit class EncodableByteArray(bytes: Array[Byte]) {
    def toB64() = new String(Base64.encodeBase64(bytes), "ASCII")
  }

  implicit class DecodableString(str: String) {
    def fromB64() = Base64.decodeBase64(str.getBytes("ASCII"))
  }

}