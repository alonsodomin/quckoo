package io.quckoo.serialization.json

import io.quckoo.id.JobId

import upickle.Js
import upickle.default.{Reader => JsonReader, Writer => JsonWriter, _}

/**
  * Created by alonsodomin on 04/07/2016.
  */
trait CustomModel {

  implicit val jobIdW: JsonWriter[JobId] = JsonWriter[JobId] {
    jobId => Js.Str(jobId.toString)
  }

  implicit val jobIdR: JsonReader[JobId] = JsonReader[JobId] {
    case Js.Str(id) => JobId(id)
  }

}
