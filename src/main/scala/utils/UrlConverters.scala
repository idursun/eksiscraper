package utils

import java.net.URL

object UrlConverters {
  implicit def toURL(url:String):URL = new URL(url)
}
