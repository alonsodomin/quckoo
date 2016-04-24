/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.resolver

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

  override def toString: String = s"Patterns(ivyPatterns = $ivyPatterns, artifactPatterns = $artifactPatterns, mavenCompatible = $mavenCompatible)"

}
