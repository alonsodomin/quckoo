// Comment to get more information during initialization
logLevel := Level.Warn

// Project management plugins
addSbtPlugin("de.heikoseeberger" % "sbt-header"    % "3.0.1")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"       % "1.1.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"   % "1.0.6")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"  % "2.0")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt"  % "1.12")

// Web plugins
addSbtPlugin("org.scala-js"     % "sbt-scalajs"              % "0.6.20")
addSbtPlugin("org.scala-native" % "sbt-crossproject"         % "0.2.2")
addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.2.2")
addSbtPlugin("com.vmunier"      % "sbt-web-scalajs"          % "1.0.6")
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify"              % "1.4.10")
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl"                % "1.3.7")

// Server side plugins
addSbtPlugin("io.spray"          % "sbt-revolver"        % "0.9.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.2.2")
addSbtPlugin("com.typesafe.sbt"  % "sbt-multi-jvm"       % "0.4.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-aspectj"         % "0.11.0")
addSbtPlugin("com.tapad"         % "sbt-docker-compose"  % "1.0.27")

// Code generators
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
