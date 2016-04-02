package io.quckoo

/**
 * Created by domingueza on 06/07/15.
 */
trait Job extends Serializable {

  def execute(): Any

}
