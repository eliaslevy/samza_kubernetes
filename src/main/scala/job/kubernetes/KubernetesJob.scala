package eliaslevy.samza.job.kubernetes

import java.io._
import scala.collection.JavaConverters._

import org.apache.samza.SamzaException
import org.apache.samza.config.Config
import org.apache.samza.config.JobConfig
import org.apache.samza.config.JobConfig.Config2Job
import org.apache.samza.config.ShellCommandConfig
import org.apache.samza.config.ShellCommandConfig.Config2ShellCommand
import org.apache.samza.coordinator.JobCoordinator
import org.apache.samza.job.ApplicationStatus
import org.apache.samza.job.ApplicationStatus.Running
import org.apache.samza.job.StreamJob
import org.apache.samza.job.model.JobModel
import org.apache.samza.serializers.model.SamzaObjectMapper

import eliaslevy.samza.config.KubernetesConfig
import eliaslevy.samza.config.KubernetesConfig.Config2Kubernetes

object KubernetesJob {
  val DEFAULT_CONTAINER_MEM = "1Gi"
  val DEFAULT_CPU_CORES     = "1"
}

/**
 * Writes a Kubernetes pod spec.
 */
class KubernetesJob(config: Config) extends StreamJob {
	import KubernetesJob._
  
  def submit: KubernetesJob = {
  	val containerImage = config.getContainerImage match {
  		case Some(image) => image
  		case _ => throw new SamzaException("no container image defined")
  	}

  	val outputFilename = config.getResourcesOutputFilename match {
  		case Some(filename) => filename
  		case _ => throw new SamzaException("no output file for Kubernetes resources defined")
  	}

  	val writer = new BufferedWriter(new FileWriter(new File(outputFilename)))

    val coordinator = JobCoordinator(config, config.getContainerCount.getOrElse(1))
    val jsonJob = SamzaObjectMapper.getObjectMapper().writeValueAsString(coordinator.jobModel)

    // println(jsonJob)

    val nodeSelector = 	Map(
    	config.getContainerNodeSelector
    	getOrElse(",") 
    	split(",") 
    	map( tagVal => tagVal split ":" ) 
    	map { case Array(k,v) => (k,v) }
    	: _*
    ) 

    val containers = coordinator.jobModel.getContainers

    val replicationControllers = containers.asScala.keys.toSeq.sorted.map(
    	containerId => KubernetesReplicationController.toYAML(
	    	config.getName.getOrElse(throw new SamzaException("job name not defined")),
				containerId,
				containerImage,
				nodeSelector,
				config.getContainerMemory.getOrElse(DEFAULT_CONTAINER_MEM),
				config.getContainerCpuCores.getOrElse(DEFAULT_CPU_CORES),
				config.getTaskOpts.getOrElse(""),
				jsonJob
			)
    )

    val rcYAML = replicationControllers.mkString 
    writer.write(rcYAML)
    writer.close

    //println(rcYAML)
  	this
  }

  def kill: KubernetesJob = this

  def waitForFinish(timeoutMs: Long): ApplicationStatus = Running

  def waitForStatus(status: ApplicationStatus, timeoutMs: Long): ApplicationStatus = Running

  def getStatus: ApplicationStatus = Running
}