package eliaslevy.samza.job.kubernetes

import org.apache.samza.config.Config
import org.apache.samza.job.StreamJobFactory

class KubernetesJobFactory extends StreamJobFactory {
  def getJob(config: Config) = {
    new KubernetesJob(config)
  }
}