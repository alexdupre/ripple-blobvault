import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "ripple-blobvault"
  val appVersion      = "1.0"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "org.reactivemongo" %% "play2-reactivemongo" % "0.9"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
