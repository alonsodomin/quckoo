import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.{JavaAppPackaging, JavaServerAppPackaging}
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin

abstract class QuckooPackager extends AutoPlugin {

  protected val linuxHomeLocation = "/opt/quckoo"

  override def requires: Plugins = DockerPlugin

  val defaultJvmParams = Seq(
    "-Dconfig.file=${app_home}/../conf/application.conf",
    "-Dlog4j.configurationFile=${app_home}/../conf/log4j2.xml"
  )

  protected lazy val defaultPackagingSettings: Seq[Def.Setting[_]] = Seq(
    bashScriptExtraDefines ++= defaultJvmParams.map(p => s"""addJava "$p""""),
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

  object autoImport { }

  override def requires: Plugins = super.requires && JavaAppPackaging

  override lazy val projectSettings = defaultPackagingSettings

}

object QuckooServerPackager extends QuckooPackager {

  object autoImport {
    val aspectjWeaver = taskKey[File]("Aspectj Weaver jar file")
  }
  import autoImport._

  override def requires: Plugins = super.requires && JavaServerAppPackaging

  override lazy val projectSettings = defaultPackagingSettings ++ Seq(
    aspectjWeaver := findAspectjWeaver(update.value),
    mappings in Universal += aspectjWeaver.value -> "bin/aspectjweaver.jar",
    bashScriptExtraDefines += """addJava "-javaagent:${app_home}/aspectjweaver.jar"""",
    dockerExposedVolumes ++= Seq(
      s"$linuxHomeLocation/resolver/cache",
      s"$linuxHomeLocation/resolver/local"
    )
  )

  private[this] val aspectjWeaverFilter: DependencyFilter =
    configurationFilter(Aspectj.name) && artifactFilter(name = "aspectjweaver", `type` = "jar")

  private[this] def findAspectjWeaver(report: UpdateReport) =
    report.matching(aspectjWeaverFilter).head

}