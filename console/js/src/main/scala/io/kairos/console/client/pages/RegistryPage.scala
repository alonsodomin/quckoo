package io.kairos.console.client.pages

import io.kairos.JobSpec
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.validation._
import org.widok._
import org.widok.bindings.{Bootstrap, FontAwesome => fa, HTML}
import org.widok.html._
import org.widok.validation.Validations._
import pl.metastack.metarx.{Buffer, Var}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

/**
  * Created by alonsodomin on 17/02/2016.
  */
trait RegistryState {
  import validation._

  val jobName = Var("")
  val description = Var("")
  val groupId = Var("")
  val artifactId = Var("")
  val version = Var("")
  val jobClass = Var("")

  val registeredJobs = Buffer[JobSpec]()

  implicit val validator = Validator(
    jobName -> Seq(RequiredValidation()),
    groupId -> Seq(RequiredValidation()),
    artifactId -> Seq(RequiredValidation()),
    groupId -> Seq(RequiredValidation()),
    version -> Seq(RequiredValidation()),
    jobClass -> Seq(RequiredValidation())
  )

}

case class RegistryPage() extends PrivatePage with RegistryState {
  import Bootstrap._
  import ValidationBridge._

  val registerForm: ModalBuilder = ModalBuilder(
    Modal.Header(Modal.Close(registerForm.dismiss), Modal.Title("Register new job")),
    Modal.Body(HTML.Form(
      FormGroup(
        InputGroup(
          ControlLabel("Job name").forId("jobName"),
          Input.Text().id("jobName").placeholder("Job name").bind(jobName),
          validator.message(jobName)
        )
      ).validated(jobName),
      FormGroup(
        InputGroup(
          ControlLabel("Description").forId("description"),
          Input.Text().id("description").placeholder("Description").bind(description)
        )
      ),
      FormGroup(
        ControlLabel("Artifact"),
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
          ControlLabel("Job Class").forId("jobClass"),
          Input.Text().id("jobClass").placeholder("Job Class").bind(jobClass)
        )
      )
    ).id("registerJobForm")),
    Modal.Footer(
      Button("Submit").style(Style.Primary).enabled(validator.valid),
      Button("Cancel").onClick(_ => registerForm.dismiss())
    )
  ).id("registerModal")

  override def body(route: InstantiatedRoute): View = Inline(
    HTML.Heading.Level2("Registry"),
    registerForm,
    Button(fa.Bookmark(), "Register Job").
      style(Style.Default).
      onClick(_ => registerForm.open()),
    Table(
      HTML.Table.Head(
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
