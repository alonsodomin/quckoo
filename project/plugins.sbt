// Comment to get more information during initialization
logLevel := Level.Warn

// Project management plugins
addSbtPlugin("de.heikoseeberger" % "sbt-header"    % "2.0.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.5.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"       % "1.0.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"   % "1.0.6")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"  % "2.0")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt"  % "1.10")

// Web plugins
addSbtPlugin("org.scala-js"     % "sbt-scalajs"              % "0.6.19")
addSbtPlugin("org.scala-native" % "sbt-crossproject"         % "0.2.0")
addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.2.0")
addSbtPlugin("com.vmunier"      % "sbt-web-scalajs"          % "1.0.5")
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify"              % "1.4.9")
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl"                % "1.3.4")

// Server side plugins
addSbtPlugin("io.spray"          % "sbt-revolver"        % "0.8.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.2.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-multi-jvm"       % "0.3.11")
addSbtPlugin("com.lightbend.sbt" % "sbt-aspectj"         % "0.11.0")
addSbtPlugin("com.tapad"         % "sbt-docker-compose"  % "1.0.25")

// Code generators
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
