import com.typesafe.sbt.packager.archetypes.JavaAppPackaging.autoImport._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._

object Packaging {

  private[this] val linuxHomeLocation = "/opt/quckoo"

  lazy val universalSettings = Seq(
    bashScriptExtraDefines ++= Seq(
      """addJava "-Dlog4j.configurationFile=${app_home}/../conf/log4j2.xml""""
    )
  )

  lazy val universalServerSettings = Seq(
    bashScriptExtraDefines ++= Seq(
      """addJava "-Dconfig.file=${app_home}/../conf/application.conf"""",
      """addJava "-Dlog4j.configurationFile=${app_home}/../conf/log4j2.xml""""
    )
  )

  lazy val dockerSettings = Seq(
    dockerRepository := Some("quckoo"),
    dockerUpdateLatest := true,
    dockerExposedVolumes := Seq(
      s"$linuxHomeLocation/conf",
      s"$linuxHomeLocation/resolver/cache",
      s"$linuxHomeLocation/resolver/repository"
    ),
    defaultLinuxInstallLocation in Docker := linuxHomeLocation,
    dockerCommands += Cmd("ENV", "QUCKOO_HOME", linuxHomeLocation)
  )

  lazy val masterDockerSettings = dockerSettings ++ Seq(
    dockerExposedPorts := Seq(2551, 8095),
    dockerCommands ++= Seq(
      ExecCmd("RUN mkdir -p resolver/cache"),
      ExecCmd("RUN mkdir -p resolver/local")
    )
  )

  lazy val workerDockerSettings = dockerSettings ++ Seq(
    dockerExposedPorts := Seq(5001)
  )

}