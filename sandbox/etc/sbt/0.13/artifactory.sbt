publishTo := {
  val baseUrl = "http://192.168.50.25:8081/artifactory/"
  val artifactoryUrl = if (isSnapshot.value) {
    baseUrl + "libs-snapshot-local;build.timestamp=" + System.currentTimeMillis()
  } else {
    baseUrl + "libs-release-local"
  }
  Some("Artifactory Realm" at artifactoryUrl)
}
credentials += Credentials("Artifactory Realm", "192.168.50.25", "admin", "password")