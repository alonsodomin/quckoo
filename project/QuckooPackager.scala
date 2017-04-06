import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.{JavaAppPackaging, JavaServerAppPackaging}
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin

trait QuckooPackagerKeys {
  val extraJvmParams = settingKey[Seq[String]]("Extra JVM parameters")
}
object QuckooPackagerKeys extends QuckooPackagerKeys

abstract class QuckooPackager extends AutoPlugin {

  protected val linuxHomeLocation = "/opt/quckoo"

  override def requires: Plugins = DockerPlugin

  protected val defaultJvmParams = Seq(
    "-Dconfig.file=${app_home}/../conf/application.conf",
    "-Dlog4j.configurationFile=${app_home}/../conf/log4j2.xml"
  )

  protected lazy val defaultPackagingSettings: Seq[Def.Setting[_]] = Seq(
    QuckooPackagerKeys.extraJvmParams := defaultJvmParams,
    bashScriptExtraDefines ++= QuckooPackagerKeys.extraJvmParams.value.map(p => s"""addJava "$p""""),
    maintainer in Docker := "A. Alonso Dominguez",
    dockerRepository := Some("quckoo"),
    dockerUpdateLatest := true,
    dockerExposedVolumes := Seq(
      s"$linuxHomeLocation/conf"
    ),
    defaultLinuxInstallLocation in Docker := linuxHomeLocation,
    dockerCommands ++= Seq(
      Cmd("ENV", "QUCKOO_HOME", linuxHomeLocation)
    ),
    packageName := name.value,
    packageName in Universal := s"${moduleName.value}-${version.value}",
    executableScriptName := moduleName.value
  )

}

object QuckooAppPackager extends QuckooPackager {

  val autoImport = QuckooPackagerKeys

  override def requires: Plugins = super.requires && JavaAppPackaging

  override lazy val projectSettings = defaultPackagingSettings

}

object QuckooServerPackager extends QuckooPackager {
  import QuckooAppKeys._

  val autoImport = QuckooPackagerKeys
  import autoImport._

  override def requires: Plugins = super.requires && JavaServerAppPackaging && QuckooApp

  override lazy val projectSettings = defaultPackagingSettings ++ Seq(
    mappings in Universal ++= Seq(
      aspectjWeaver.value -> "bin/aspectjweaver.jar",
      sigarLoader.value   -> "bin/sigar-loader.jar"
    ),
    extraJvmParams := defaultJvmParams ++ Seq(
      "-javaagent:${app_home}/aspectjweaver.jar",
      "-javaagent:${app_home}/sigar-loader.jar"
    ),
    dockerExposedVolumes ++= Seq(
      s"$linuxHomeLocation/resolver/cache",
      s"$linuxHomeLocation/resolver/local"
    )
  )

}