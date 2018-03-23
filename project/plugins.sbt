// Comment to get more information during initialization
logLevel := Level.Warn

// Project management plugins
addSbtPlugin("de.heikoseeberger" % "sbt-header"    % "4.0.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"       % "1.1.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"   % "1.0.7")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"  % "2.0")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt"  % "1.15")
addSbtPlugin("com.dwijnand"      % "sbt-travisci"  % "1.1.1")

// Web plugins
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "0.6.22")
addSbtPlugin("org.portable-scala" % "sbt-crossproject"         % "0.3.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.3.1")
addSbtPlugin("com.vmunier"        % "sbt-web-scalajs"          % "1.0.6")
addSbtPlugin("org.irundaia.sbt"   % "sbt-sassify"              % "1.4.11")
addSbtPlugin("com.typesafe.sbt"   % "sbt-twirl"                % "1.3.15")

// Server side plugins
addSbtPlugin("io.spray"          % "sbt-revolver"        % "0.9.1")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.3.3")
addSbtPlugin("com.typesafe.sbt"  % "sbt-multi-jvm"       % "0.4.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-aspectj"         % "0.11.0")
addSbtPlugin("com.tapad"         % "sbt-docker-compose"  % "1.0.34")

// Code generators
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
