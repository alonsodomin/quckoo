package io.kairos.console.client.pages

import io.kairos.JobSpec
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.validation._
import io.kairos.console.client.widget.widok.Placeholder
import io.kairos.id.ArtifactId
import org.scalajs.dom
import org.widok._
import org.widok.bindings.{Bootstrap, FontAwesome => fa, HTML}
import org.widok.html._
import org.widok.validation.Validations._
import pl.metastack.metarx.{Buffer, Opt, Var}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

/**
  * Created by alonsodomin on 17/02/2016.
  */
object JobSpecDialog {

  trait State {
    import validation._

    val jobName = Var("")
    val description = Opt[String]()
    val groupId = Var("")
    val artifactId = Var("")
    val version = Var("")
    val jobClass = Var("")

    implicit val validator = Validator(
      jobName -> Seq(RequiredValidation()),
      groupId -> Seq(RequiredValidation()),
      artifactId -> Seq(RequiredValidation()),
      groupId -> Seq(RequiredValidation()),
      version -> Seq(RequiredValidation()),
      jobClass -> Seq(RequiredValidation())
    )
  }

  object View extends State {
    import Bootstrap._
    import ValidationBridge._

    val message = Opt[Widget[_]]()

    val descriptionChannel = description.biMap(
      _.getOrElse(""),
      (str: String) => if(!str.isEmpty) Some(str) else None
    )

    def submitForm(event: dom.Event) = {
      import scalaz.{Failure => Invalid, Success => Valid}

      modal.dismiss()

      val artifact = ArtifactId(groupId.get, artifactId.get, version.get)
      val jobSpec = JobSpec(jobName.get, description.get, artifact, jobClass.get)

      ClientApi.registerJob(jobSpec) onComplete {
        case Success(response) => response match {
          case Valid(jobId) =>
            message := Alert(HTML.Container.Inline(
              s"Job successfully created: $jobId"
            )).style(Style.Success)
          case Invalid(reason) =>
            dom.console.log(reason.toString())
            message := Alert(HTML.List.Unordered(
              reason.list.toList.map(fault => HTML.List.Item(fault.toString())): _*
            )).style(Style.Danger)
        }
        case Failure(_) =>
      }
    }

    val modal: ModalBuilder = ModalBuilder(
      Modal.Header(Modal.Close(modal.dismiss), Modal.Title("Register new job")),
      Modal.Body(
        Placeholder(message),
        FormGroup(
          InputGroup(
            ControlLabel("Job name *").forId("jobName"),
            Input.Text().id("jobName").placeholder("Job name").bind(jobName),
            validator.message(jobName)
          )
        ).validated(jobName),
        /*FormGroup(
          InputGroup(
            ControlLabel("Description").forId("description"),
            Input.Text().id("description").placeholder("Description").bind(descriptionChannel)
          )
        ),*/
        FormGroup(
          ControlLabel("Artifact *"),
          Grid.Row(
            Grid.Column(
              FormGroup(
                Input.Text().id("groupId").placeholder("Group Id").bind(groupId),
                validator.message(groupId)
              ).validated(groupId)
            ).column(Size.Medium, 4),
            Grid.Column(
              FormGroup(
                Input.Text().id("artifactId").placeholder("Artifact Id").bind(artifactId),
                validator.message(artifactId)
              ).validated(artifactId)
            ).column(Size.Medium, 4),
            Grid.Column(
              FormGroup(
                Input.Text().id("version").placeholder("Version").bind(version),
                validator.message(version)
              ).validated(version)
            ).column(Size.Medium, 4)
          )
        ),
        FormGroup(
          InputGroup(
            ControlLabel("Job Class *").forId("jobClass"),
            Input.Text().id("jobClass").placeholder("Job Class").bind(jobClass),
            validator.message(jobClass)
          )
        ).validated(jobClass)
      ),
      Modal.Footer(
        Button("Submit").style(Style.Primary).enabled(validator.valid),
        Button("Cancel").onClick(_ => modal.dismiss())
      )
    ).id("registerModal")
  }

  def apply() = View.modal

}

case class RegistryPage() extends PrivatePage {
  import Bootstrap._

  val registerDlg = JobSpecDialog()
  val registeredJobs = Buffer[JobSpec]()

  override def body(route: InstantiatedRoute): View = Inline(
    HTML.Heading.Level2("Registry"),
    HTML.Form(registerDlg).
      id("registerJobForm").
      onSubmit(JobSpecDialog.View.submitForm),
    Button(fa.Bookmark(), "Register Job").
      style(Style.Default).
      onClick(_ => registerDlg.open()),
    Table(
      thead(
        HTML.Table.Row(
          HTML.Table.HeadColumn("Name"),
          HTML.Table.HeadColumn("Description"),
          HTML.Table.HeadColumn("ArtifactId"),
          HTML.Table.HeadColumn("JobClass")
        )
      ),
      tbody(
        registeredJobs.map(job => {
          Table.Row(
            Table.Column(job.displayName),
            Table.Column(HTML.Text(job.description.getOrElse(""))),
            Table.Column(job.artifactId.toString()),
            Table.Column(job.jobClass)
          )
        })
      )
    ).striped(true).hover(true)
  )

  whenReady { _ =>
    import KairosPage.ReadyResult._

    ClientApi.enabledJobs onComplete {
      case Success(jobs) =>
        registeredJobs.appendAll(jobs.values.toSeq)
      case Failure(_) =>
    }
    Continue
  }

}
