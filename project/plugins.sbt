// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("io.spray"          % "sbt-revolver"        % "0.8.0")
addSbtPlugin("org.scala-js"      % "sbt-scalajs"         % "0.6.11")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.2.0-M5")
addSbtPlugin("com.typesafe.sbt"  % "sbt-multi-jvm"       % "0.3.11")
addSbtPlugin("org.madoushi.sbt"  % "sbt-sass"            % "0.9.3")
addSbtPlugin("de.heikoseeberger" % "sbt-header"          % "1.5.1")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")

