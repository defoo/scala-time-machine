package net.defoo

import akka.actor.{Props, Actor}
import java.io.File
import org.apache.commons.io.filefilter.{TrueFileFilter, DirectoryFileFilter}
import java.util.Date
import org.apache.commons.io.FileUtils
import scala.collection.JavaConversions._
import akka.routing.RoundRobinRouter
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.text.SimpleDateFormat
import akka.event.Logging
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import org.slf4j.MDC
import scala.collection.immutable.ListMap

/**
 * Created by derek on 23/02/14.
 */

class BackupActor extends Actor {

  final val dirTimeFmt = new SimpleDateFormat("yyyy-MM-dd__HH_mm_ss.SSS_Z")

  val log = Logging(context.system, this)

  val workerRouter = context.actorOf(Props[FileCopyActor].withRouter(RoundRobinRouter(4)), name = "copyWorkerRouter")

  val reportActor = context.actorOf(Props[ReportActor], name = "BackupReporter")

  var nCopySent: Map[String, Int] = Map()
  var nResultReceived: Map[String, Int] = Map()

  var recurring = false

  def receive = {

    case BackupJob(jobId, srcDir, backupHome, fileFilter, dirFilter, actualCopy, recurring) =>
      log.info(ListMap("jobId" -> jobId, "msg" -> "Begin backup session", "recurring" -> recurring).toString)

      this.recurring = recurring

      val pastBackups = backupHome.list(DirectoryFileFilter.INSTANCE) flatMap {
        dir =>
          try {
            dirTimeFmt.parse(dir)
          } catch {
            case e: Throwable =>
              log.warning(ListMap("jobId" -> jobId, "msg" -> s"Invalid dir name from backupHome while reading for past backup", "dir" -> s"${dir}").toString)
              None
          }
          Option(new File(backupHome, dir))
      } sortBy {
        backupDir => dirTimeFmt parse backupDir.getName
      }

      // {backupHome} / {date-time} / {srcFolderName}
      val backupTargetLoc = createTargetInNewWorkspace(backupHome, srcDir.getName).getAbsolutePath

      val lastBackupDir = pastBackups.lastOption map {
        lastWorkspace => new File(lastWorkspace, srcDir.getName)
      }

      FileUtils iterateFilesAndDirs(srcDir, fileFilter, dirFilter) filter {
        f =>
          val canRead = f.canRead
          canRead match {
            case false =>
              log.warning(ListMap("jobId" -> jobId, "msg" -> s"Cannot read file", "dir" -> s"${f}").toString)
            case _ =>
          }
          canRead

      } foreach {
        srcFile =>
          val backupToFile = new File(srcFile.getAbsolutePath.replace(srcDir.getAbsolutePath, backupTargetLoc))

          val lastBackupFile = lastBackupDir map {
            lastBackupDir =>
              new File(srcFile.getAbsolutePath.replace(srcDir.getAbsolutePath, lastBackupDir.getAbsolutePath)) match {
                case f if f.exists => Option(f)
                case _ => None
              }
          }
          nCopySent += jobId -> (nCopySent.getOrElse(jobId, 0) + 1)

          actualCopy match {
            case true =>
              workerRouter ! BackupFile(jobId = jobId, src = srcFile, dest = backupToFile, lastBackup = None)
            case false =>
              workerRouter ! BackupFile(jobId = jobId, src = srcFile, dest = backupToFile, lastBackup = lastBackupFile.flatten)

          }
      }


    case copyCompleteMsg@BackupFileComplete(jobId, _, _, _, _, _) =>

      val receivedForJob = nResultReceived.getOrElse(jobId, 0) + 1
      val copySentForJob = nCopySent.getOrElse(jobId, 0)

      nResultReceived += jobId -> receivedForJob

      log.debug(ListMap("jobId" -> jobId, "copySentForJob" -> copySentForJob, "receivedForJob" -> receivedForJob).toString)

      reportActor forward copyCompleteMsg

      if (receivedForJob == copySentForJob) {
        log.info(ListMap("jobId" -> jobId, "msg" -> "End of backup session").toString)

        if (!recurring) {
          context.stop(self)
          context.system.shutdown()
        }
      }
  }

  private def createTargetInNewWorkspace(backupHome: File, targetDirName: String) = {
    val currentWorkspace = new File(backupHome, dirTimeFmt format new Date())
    val targetInworkspace = new File(currentWorkspace, targetDirName)
    targetInworkspace.mkdirs()
    targetInworkspace
  }
}
