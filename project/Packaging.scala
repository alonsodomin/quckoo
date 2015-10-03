import com.typesafe.sbt.SbtNativePackager.{Docker, Universal}
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._
import sbt.Keys._
import sbt._

object Packaging {

  lazy val universalSettings = Seq(
    bashScriptExtraDefines ++= Seq(
      """addJava "-Dconfig.file=${app_home}/../conf/application.conf""""
    ),
    mappings in Universal <++= sourceDirectory map { src =>
      val resources = src / "main" / "resources"
      val log4j = resources / "log4j2.xml"
      val referenceConf = resources / "reference.conf"
      Seq(log4j -> "conf/log4j2.xml", referenceConf -> "conf/application.conf")
    }
  )

  lazy val dockerSettings = Seq(
    dockerRepository in Docker := Some("chronos"),
    dockerExposedVolumes in Docker := Seq("/opt/chronos/conf"),
    defaultLinuxInstallLocation in Docker := "/opt/chronos"
  )

  lazy val schedulerUniversalSettings = universalSettings ++ Seq(
    bashScriptExtraDefines ++= Seq(
      """if [ ! -z "$CASSANDRA_PORT_9042_TCP_ADDR" -a ! -z "$CASSANDRA_PORT_9042_TCP_PORT" ]; then addApp --cs; addApp $CASSANDRA_PORT_9042_TCP_ADDR:$CASSANDRA_PORT_9042_TCP_PORT; fi"""
    )
  )

  lazy val schedulerDockerSettings = dockerSettings ++ Seq(
    dockerExposedPorts in Docker := Seq(2551)
  )

}