package net.defoo

import java.io.File
import akka.actor.{Props, ActorSystem}
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{FileFilterUtils => FF, RegexFileFilter, IOFileFilter, HiddenFileFilter}
import scala.collection.JavaConversions._
import java.nio.file.Files
import akka.event.Logging
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

case class BackupConfig(src: File = new File(""), backupHome: File = new File(""), ignoreHiddenFile: Boolean = true, dirIgnoreFilters: Seq[IOFileFilter] = Seq(), recurringMinute: Int = -1)


object ScalaTimeMachine extends App {

  val parser = new scopt.OptionParser[BackupConfig]("scopt") {
    head("Scala time machine", "1.0")
    help("help") text ("prints this usage text")

    opt[File]("src") action {
      (x, c) =>
        c.copy(src = x)
    } text ("Path to the directory you wish to backup")

    opt[File]("backupHome") action {
      (x, c) =>
        c.copy(backupHome = x)
    } text ("Path to the directory of where the backups go")

    opt[Unit]("ignore-hidden-file") optional() action {
      (x, c) =>
        c.copy(ignoreHiddenFile = true)
    } text ("Default to ignore hidden file")

    opt[Unit]("ignore-hidden-dir") optional() action {
      (x, c) =>
        c.copy(dirIgnoreFilters = c.dirIgnoreFilters :+ HiddenFileFilter.VISIBLE)
    } text ("Default to not process hidden directory")

    opt[Int]("recurring-interval-minute") optional() action {
      (x, c) =>
        c.copy(recurringMinute = x)
    } text ("Run backup job every n minute. Default is -1 (Run once).")


    arg[String]("<file>...") unbounded() optional() action {
      (x, c) =>
        c.copy(dirIgnoreFilters = c.dirIgnoreFilters :+ FF.notFileFilter(new RegexFileFilter(x)))

    } text ("List of java regular expressions that match the directory names you want to ignore")
  }

  parser.parse(args, BackupConfig()) map {
    config =>
      val fileFilters = config.ignoreHiddenFile match {
        case true =>
          FF and(FF.trueFileFilter, HiddenFileFilter.VISIBLE)
        case false =>
          FF.trueFileFilter
      }

      val dirFilters = FF and (config.dirIgnoreFilters: _*)


      val backupHomeDir = config.backupHome
      val logger = LoggerFactory.getLogger(this.getClass)

      backupHomeDir.mkdirs

      if (!(backupHomeDir.canRead && backupHomeDir.canWrite)) {
        logger.error(s"Cannot access backupHomeDir: ${backupHomeDir}")
        throw CannotAccessBackupHomeException(backupHomeDir)
      }

      val system = ActorSystem("BackupSystem")

      val backupMaster = system.actorOf(Props[BackupActor], name = "BackupMaster")

      config.recurringMinute match {
        case x if x > 0 =>
          val newBackupJob = BackupJob(jobId = newUUID, src = config.src, backupHomeDir = backupHomeDir, fileFilter = fileFilters, dirFilter = dirFilters, actualCopy = false, recurring = true)
          system.scheduler schedule(0 minutes, x minutes, backupMaster, newBackupJob)

        case _ =>
          val newBackupJob = BackupJob(jobId = newUUID, src = config.src, backupHomeDir = backupHomeDir, fileFilter = fileFilters, dirFilter = dirFilters, actualCopy = false, recurring = false)
          system.scheduler.scheduleOnce(0 minutes, backupMaster, newBackupJob)

      }

  } getOrElse {

  }


}
