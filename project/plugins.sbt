// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.1")

// IntelliJ plugin
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

// The Play plugin
val playVersion = "2.4.2"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % playVersion)

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")
addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.7")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.0")
//addSbtPlugin("com.github.mumoshu" % "play2-typescript" % "0.3.0-RC1")

