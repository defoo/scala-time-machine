package net.defoo

import akka.actor.Actor
import akka.event.Logging
import scala.collection.immutable.ListMap

/**
 * Created by derek on 01/03/14.
 */
class ReportActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case BackupFileComplete(jobId, src, dest, srcChecksum, backupChecksum, lastBackupChecksum) =>

      log.info(ListMap("jobId" -> jobId, "src" -> src, "srcChecksum" -> srcChecksum, "dest" -> dest, "backupChecksum" -> backupChecksum, "lastBackupChecksum" -> lastBackupChecksum).toString)

  }
}
