package eliaslevy.samza.config

import org.apache.samza.config.Config
import org.apache.samza.config.ScalaMapConfig

object KubernetesConfig {
  val CONTAINER_IMAGE           = "kubernetes.container.image"
  val CONTAINER_MEMORY          = "kubernetes.container.memory"
  val CONTAINER_CPU_CORES       = "kubernetes.container.cpu.cores"
  val CONTAINER_TERM_PERIOD_SEC = "kubernetes.pod.termination.grace.period.sec"
  val CONTAINER_COUNT           = "kubernetes.pod.count"
  val CONTAINER_NODE_SELECTOR   = "kubernetes.node.selector"
  val RESOURCES_OUTPUT_FILENAME = "kubernetes.resources.out"

  implicit def Config2Kubernetes(config: Config) = new KubernetesConfig(config)
}

class KubernetesConfig(config: Config) extends ScalaMapConfig(config) {
  def getContainerImage                             = getOption(KubernetesConfig.CONTAINER_IMAGE          )

  def getContainerMemory                            = getOption(KubernetesConfig.CONTAINER_MEMORY         )

  def getContainerCpuCores                          = getOption(KubernetesConfig.CONTAINER_CPU_CORES      )

  def getContainerTerminationPeriodSec: Option[Int] = getOption(KubernetesConfig.CONTAINER_TERM_PERIOD_SEC).map(_.toInt)

  def getContainerCount: Option[Int]                = getOption(KubernetesConfig.CONTAINER_COUNT          ).map(_.toInt)

  def getContainerNodeSelector                      = getOption(KubernetesConfig.CONTAINER_NODE_SELECTOR  )

  def getResourcesOutputFilename                    = getOption(KubernetesConfig.RESOURCES_OUTPUT_FILENAME)
}