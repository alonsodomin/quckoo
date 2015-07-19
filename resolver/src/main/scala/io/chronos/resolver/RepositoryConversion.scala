package io.chronos.resolver

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.{AbstractPatternsBasedResolver, DependencyResolver, FileSystemResolver, URLResolver}

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
private[resolver] object RepositoryConversion {
  type RepositoryConverter = PartialFunction[(Repository, IvySettings), DependencyResolver]

  def apply(repository: Repository, settings: IvySettings): DependencyResolver =
    apply(repository, settings, defaultConverter)

  def apply(repository: Repository, settings: IvySettings, converter: RepositoryConverter): DependencyResolver =
    converter((repository, settings))

  lazy val defaultConverter: RepositoryConverter = {
    case (r, settings) => r match {
      case repo: URLRepository => {
        val resolver = new URLResolver
        resolver.setName(repo.name)
        initializePatterns(resolver, repo.patterns, settings)
        resolver
      }

      case repo: FileRepository => {
        val resolver = new FileSystemResolver
        resolver.setName(repo.name)
        initializePatterns(resolver, repo.patterns, settings)
        resolver.setLocal(true)
        resolver
      }
    }
  }

  private def initializePatterns(resolver: AbstractPatternsBasedResolver, patterns: Patterns, settings: IvySettings) {
    resolver.setM2compatible(patterns.mavenCompatible)
    patterns.artifactPatterns.foreach { p => resolver.addArtifactPattern(settings substitute p) }
    patterns.ivyPatterns.foreach { p => resolver.addIvyPattern(settings substitute p) }
  }

}
