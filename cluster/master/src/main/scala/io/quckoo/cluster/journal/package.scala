package io.quckoo.cluster

import akka.persistence.query.scaladsl.{CurrentEventsByTagQuery, ReadJournal}

/**
  * Created by alonsodomin on 10/09/2016.
  */
package object journal {

  type ReadJournalRepr = ReadJournal with CurrentEventsByTagQuery

}
