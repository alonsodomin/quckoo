package io.chronos.resolver

/**
 * Created by aalonsodominguez on 18/07/15.
 */
object Patterns {

  def apply(ivyPatterns: Seq[String], artifactPatterns: Seq[String]): Patterns =
    Patterns(ivyPatterns, artifactPatterns, mavenCompatible = true)

  def apply(ivyPatterns: Seq[String], artifactPatterns: Seq[String], mavenCompatible: Boolean): Patterns =
    new Patterns(ivyPatterns, artifactPatterns, mavenCompatible)

  private[resolver] def resolvePatterns(base: String, patterns: Patterns): Patterns = {
    def resolveAll(patterns: Seq[String]): Seq[String] = patterns.map(p => resolvePattern(base, p))
    Patterns(resolveAll(patterns.ivyPatterns), resolveAll(patterns.artifactPatterns), patterns.mavenCompatible)
  }

  private def resolvePattern(base: String, pattern: String): String = {
    val normalized = base.replace('\\', '/')
    if (normalized.endsWith("/") || pattern.startsWith("/")) normalized + pattern else normalized + "/" + pattern
  }

}

final class Patterns private (val ivyPatterns: Seq[String], val artifactPatterns: Seq[String], val mavenCompatible: Boolean) {

  def mavenStyle(): Patterns = Patterns(ivyPatterns, artifactPatterns, mavenCompatible = true)
  def withArtifacts(patterns: String*): Patterns = Patterns(ivyPatterns, patterns ++ artifactPatterns, mavenCompatible)
  def withIvys(patterns: String*): Patterns = Patterns(ivyPatterns ++ patterns, artifactPatterns, mavenCompatible)

}
