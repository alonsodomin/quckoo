package io.chronos.worker

import java.nio.file.Path

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem

/**
 * Created by aalonsodominguez on 16/07/15.
 */
class ModuleRepository(val system: RepositorySystem, val workDir: Path) {

  val defaultRepositorySystemSession = MavenRepositorySystemUtils.newSession()

}
