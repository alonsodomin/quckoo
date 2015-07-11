package io.chronos

/**
  * Created by domingueza on 10/07/15.
  */
trait JobRepository {

   def availableSpecs: Seq[JobSpec]

   def publishSpec(jobSpec: JobSpec): Unit

 }
