// Comment to get more information during initialization
logLevel := Level.Warn

// Project management plugins
addSbtPlugin("de.heikoseeberger" % "sbt-header"    % "5.2.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.6.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"       % "1.1.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"   % "1.0.11")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"  % "3.8")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"  % "2.0.5")
addSbtPlugin("com.dwijnand"      % "sbt-travisci"  % "1.2.0")

// Web plugins
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "0.6.32")
addSbtPlugin("org.portable-scala" % "sbt-crossproject"         % "0.6.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")
addSbtPlugin("org.irundaia.sbt"   % "sbt-sassify"              % "1.4.13")
addSbtPlugin("com.typesafe.sbt"   % "sbt-twirl"                % "1.4.2")
addSbtPlugin("ch.epfl.scala"      % "sbt-web-scalajs-bundler"  % "0.15.0-0.6")

// Server side plugins
addSbtPlugin("io.spray"          % "sbt-revolver"        % "0.9.1")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.4.1")
addSbtPlugin("com.typesafe.sbt"  % "sbt-multi-jvm"       % "0.4.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-aspectj"         % "0.11.0")
addSbtPlugin("com.tapad"         % "sbt-docker-compose"  % "1.0.34")

// Code generators
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
