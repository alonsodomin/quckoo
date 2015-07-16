package io.chronos.worker

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.impl.DefaultServiceLocator.ErrorHandler
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.{DefaultRepositorySystemSession, RepositorySystem}

/**
 * Created by aalonsodominguez on 16/07/15.
 */
object ChronosRepositorySystem {

  def repositorySystem: RepositorySystem = {
    val serviceLocator = MavenRepositorySystemUtils.newServiceLocator()
    serviceLocator.addService(classOf[RepositoryConnectorFactory], classOf[BasicRepositoryConnectorFactory])
    serviceLocator.addService(classOf[TransporterFactory], classOf[HttpTransporterFactory])
    serviceLocator.setErrorHandler(new ErrorHandler {
      override def serviceCreationFailed(serviceType: Class[_], serviceImpl: Class[_], cause: Throwable): Unit = {
        cause.printStackTrace()
      }
    })
    serviceLocator.getService(classOf[RepositorySystem])
  }

  def repositorySession(repositorySystem: RepositorySystem): DefaultRepositorySystemSession = {
    val repositorySession = MavenRepositorySystemUtils.newSession()

    val localRepository = new LocalRepository("target/local-repo")
    repositorySession.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(repositorySession, localRepository))

    // TODO Add transfer listeners
    //repositorySession.setTransferListener(new ConsoleTransfe)

    repositorySession
  }

}
