package io.quckoo.resolver

import java.net.{URL, URLClassLoader}
import java.security.{AllPermission, CodeSource, Permission, PermissionCollection}
import java.util

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 25/07/15.
 */
object ArtifactClassLoader {

  private val allPermissions = new PermissionCollection {

    private val allPermission = new AllPermission()

    override def implies(permission: Permission): Boolean = true

    override def elements(): util.Enumeration[Permission] = util.Collections.enumeration(List(allPermission))

    override def add(permission: Permission): Unit = ()
  }

}

class ArtifactClassLoader(urls: Array[URL], parent: ClassLoader) extends URLClassLoader(urls, parent) {
  import ArtifactClassLoader._

  private val systemClassLoader = ClassLoader.getSystemClassLoader

  def this(urls: Array[URL]) = this(urls, null)

  override def getResource(name: String): URL = {
    var resource: URL = null
    if (systemClassLoader != null) {
      resource = systemClassLoader.getResource(name)
    }
    if (resource == null) {
      resource = findResource(name)
      if (resource == null) {
        resource = super.getResource(name)
      }
    }
    resource
  }

  override def loadClass(name: String, resolve: Boolean): Class[_] = this.synchronized {
    var clazz = findLoadedClass(name)
    if (clazz == null) {
      if (systemClassLoader != null) {
        try {
          clazz = systemClassLoader.loadClass(name)
        } catch {
          case ex: ClassNotFoundException => // ignore
        }
      }
      if (clazz == null) {
        try {
          clazz = findClass(name)
        } catch {
          case ex: ClassNotFoundException =>
            clazz = super.loadClass(name, resolve)
        }
      }
    }
    if (resolve) {
      resolveClass(clazz)
    }
    clazz
  }

  override def getPermissions(codesource: CodeSource): PermissionCollection = allPermissions

}
