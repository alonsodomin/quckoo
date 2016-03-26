package io.quckoo.console

/**
  * Created by alonsodomin on 26/03/2016.
  */
sealed trait ConsoleRoute
object ConsoleRoute {
  case object RootRoute extends ConsoleRoute
  case object DashboardRoute extends ConsoleRoute
  case object LoginRoute extends ConsoleRoute
  case object RegistryRoute extends ConsoleRoute
  case object SchedulerRoute extends ConsoleRoute
}
