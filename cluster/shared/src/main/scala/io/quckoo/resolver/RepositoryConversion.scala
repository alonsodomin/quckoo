package io.quckoo.resolver

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver._
import org.slf4s.Logging

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
private[resolver] object RepositoryConversion extends Logging {
  type RepositoryConverter = PartialFunction[(Repository, IvySettings), DependencyResolver]

  def apply(repository: Repository, settings: IvySettings): DependencyResolver =
    apply(repository, settings, defaultConverter)

  def apply(repository: Repository, settings: IvySettings, converter: RepositoryConverter): DependencyResolver =
    converter((repository, settings))

  lazy val defaultConverter: RepositoryConverter = {
    case (r, settings) => r match {
      case MavenRepository(name, url) =>
        def root: String = {
          val urlAsString = url.toURI.normalize().toURL.toString
          if (urlAsString.endsWith("/")) urlAsString
          else urlAsString + "/"
        }

        val resolver = new IBiblioResolver
        resolver.setName(name)
        resolver.setRoot(root)
        resolver.setM2compatible(true)
        //resolver.setPattern(Repository.mavenStyleBasePattern)
        log.info(s"Configured Maven repository $name at $url")
        resolver

      case repo: URLRepository =>
        val resolver = new URLResolver
        resolver.setName(repo.name)
        initializePatterns(resolver, repo.patterns, settings)
        log.info(s"Configured URL repository ${repo.name}.")
        resolver

      case repo: FileRepository =>
        val resolver = new FileSystemResolver
        resolver.setName(repo.name)
        initializePatterns(resolver, repo.patterns, settings)
        resolver.setLocal(true)
        log.info(s"Configured File repository ${repo.name}.")
        resolver
    }
  }

  private def initializePatterns(resolver: AbstractPatternsBasedResolver, patterns: Patterns, settings: IvySettings) {
    resolver.setM2compatible(patterns.mavenCompatible)
    patterns.artifactPatterns.foreach { p => resolver.addArtifactPattern(settings substitute p) }
    patterns.ivyPatterns.foreach { p => resolver.addIvyPattern(settings substitute p) }
  }

}
