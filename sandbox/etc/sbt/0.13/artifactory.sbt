publishTo := Some("Artifactory Realm" at "http://192.168.50.25:8081/artifactory/libs-snapshot-local")
credentials += Credentials("Artifactory Realm", "192.168.50.25", "admin", "password")