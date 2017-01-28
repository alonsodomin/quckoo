import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.{CmdLike, Cmd, ExecCmd}
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.Universal

import sbt._
import Keys._

object Packaging {

  private[this] val linuxHomeLocation = "/opt/quckoo"

  private[this] lazy val universalSettings = Seq(
    bashScriptExtraDefines ++= Seq(
      """addJava "-Dlog4j.configurationFile=${app_home}/../conf/log4j2.xml""""
    )
  )

  private[this] lazy val universalServerSettings = Seq(
    bashScriptExtraDefines ++= Seq(
      """addJava "-Dconfig.file=${app_home}/../conf/application.conf"""",
      """addJava "-Dlog4j.configurationFile=${app_home}/../conf/log4j2.xml""""
    )
  )

  private[this] lazy val baseDockerSettings = Seq(
    maintainer in Docker := "A. Alonso Dominguez",
    dockerRepository := Some("quckoo"),
    dockerUpdateLatest := true,
    dockerExposedVolumes := Seq(
      s"$linuxHomeLocation/conf"
    ),
    defaultLinuxInstallLocation in Docker := linuxHomeLocation
  )

  private[this] val dumbInitLocation = "/usr/local/bin/dumb-init"
  private[this] val dumbInitVersion = "1.2.0"

  private[this] def withDumbInit(cmds: Seq[CmdLike]) = {
    val installDumbInit = Seq(
      ExecCmd("RUN", "wget", "--quiet", "-O", dumbInitLocation,
        s"https://github.com/Yelp/dumb-init/releases/download/v$dumbInitVersion/dumb-init_${dumbInitVersion}_amd64"
      ),
      ExecCmd("RUN", "chmod", "+x", dumbInitLocation)
    )

    val userCmdIdx = Some(cmds.indexWhere {
      case Cmd("USER", _) => true
      case _              => false
    }).filter(_ >= 0).getOrElse(0)

    val (rootCmds, userCmds) = cmds.splitAt(userCmdIdx)

    rootCmds ++ installDumbInit ++ userCmds
  }

  private[this] lazy val serverDockerSettings = baseDockerSettings ++ Seq(
    dockerExposedVolumes ++= Seq(
      s"$linuxHomeLocation/resolver/cache",
      s"$linuxHomeLocation/resolver/local"
    ),
    dockerCommands := withDumbInit(dockerCommands.value) ++ Seq(
      Cmd("ENV", "QUCKOO_HOME", linuxHomeLocation)
    ),
    dockerEntrypoint := Seq(dumbInitLocation, "--") ++ dockerEntrypoint.value
  )

  lazy val masterSettings = universalServerSettings ++ serverDockerSettings ++ Seq(
    packageName := "master",
    packageName in Universal := s"quckoo-master-${version.value}",
    executableScriptName := "master",
    dockerExposedPorts := Seq(2551, 8095)
  )

  lazy val workerSettings = universalServerSettings ++ serverDockerSettings ++ Seq(
    packageName := "worker",
    packageName in Universal := s"quckoo-worker-${version.value}",
    executableScriptName := "worker",
    dockerExposedPorts := Seq(5001)
  )

  lazy val exampleProducersSettings = universalSettings ++ baseDockerSettings ++ Seq(
    executableScriptName := "examples"
  )

}
