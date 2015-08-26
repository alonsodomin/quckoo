import com.typesafe.sbt.SbtNativePackager.{Docker, Universal}
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Keys._
import sbt._

object Packaging {

  lazy val universal = mappings in Universal <++= sourceDirectory map { src =>
    val resources = src / "main" / "resources"
    val log4j = resources / "log4j2.xml"
    val referenceConf = resources / "reference.conf"
    Seq(log4j -> "conf/log4j2.xml", referenceConf -> "conf/application.conf")
  }

  lazy val docker = Seq(
    dockerRepository in Docker := Some("chronos"),
    dockerExposedPorts in Docker := Seq(8090),
    dockerExposedVolumes in Docker := Seq("/opt/docker/conf")
  )

}