import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.autoImport._

import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._

import QuckooAppKeys._

object QuckooMultiJvmTesting extends AutoPlugin {

  override def requires: Plugins = QuckooApp && SbtMultiJvm && HeaderPlugin

  override def projectConfigurations: Seq[Configuration] = Seq(MultiJvm)

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-multi-node-testkit"  % Version.akka.main,
        "org.scoverage"     %% "scalac-scoverage-runtime" % Version.scoverage
      ).map(_ % MultiJvm),
      parallelExecution in MultiJvm := false,
      jvmOptions in MultiJvm := (sigarLoaderOptions in Test).value :+ "-Xmx512M"
    ) ++ headerSettings(MultiJvm)

}
