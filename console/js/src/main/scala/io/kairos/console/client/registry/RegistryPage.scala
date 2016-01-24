package io.kairos.console.client.registry

import io.kairos._
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.layout.{Notification, NotificationDisplay}
import io.kairos.id.JobId
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object RegistryPage {

  object Style extends StyleSheet.Inline {

    val content = style()
  }

  case class State(notifications: Seq[Notification] = Seq(),
                   specs: Map[JobId, JobSpec] = Map.empty)

  class RegistryBackend($: BackendScope[Unit, State]) {

    def handleJobSubmit(jobSpec: JobSpec): Callback = {

      def jobRejectedMsg(state: State, cause: Faults): State = {
        def resolutionFailed = Notification.error {
          <.div(
            <.p("Dependency resolution failed:"),
            cause.list.toList.map {
              case UnresolvedDependency(artifactId) =>
                <.li(s"Unresolved dependency: $artifactId")
              case error: Fault => <.li(error.toString())
            }
          )
        }

        state.copy(notifications = state.notifications :+ resolutionFailed)
      }

      def errorMsg(state: State, t: Throwable): State =
        state.copy(notifications = state.notifications :+ Notification.error(t))

      def successMsg(state: State, jobId: JobId): State =
        state.copy(notifications = state.notifications :+ Notification.success("Job registered: " + jobId))

      def performSubmit(): Future[Callback] = {
        import scalaz._
        ClientApi.registerJob(jobSpec).map {
          case Failure(cause) => $.modState(s => jobRejectedMsg(s, cause))
          case Success(jobId) => $.modState { s =>
            successMsg(s, jobId).copy(specs = s.specs + (jobId -> jobSpec))
          }
        } recover {
          case t: Throwable => $.modState(s => errorMsg(s, t))
        }
      }

      println(s"Submitting jobspec: $jobSpec")
      $.modState(st => st.copy(notifications = Seq())) >> Callback.future(performSubmit())
    }

    def render(state: State) =
      <.div(Style.content,
        <.h2("Registry"),
        NotificationDisplay(state.notifications),
        JobForm(handleJobSubmit),
        JobSpecList(state.specs)
      )

  }

  private[this] val component = ReactComponentB[Unit]("RegistryPage").
    initialState(State()).
    renderBackend[RegistryBackend].
    componentDidMount($ => Callback.future {
      ClientApi.enabledJobs map { specMap =>
        $.modState(_.copy(specs = specMap))
      }
    }).buildU

  def apply() = component()

}
