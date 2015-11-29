samza_kubernetes
================

This is a proof of concept `SamzaJob` implementation that enables execution of Samza jobs in the Kubernetes container orchestration system.

### Overview

The `SamzaJob` in this implementation doesn't directly execute the job in Kubernetes.  Rather, it generates a set of YAML files representing Kubernetes resources that can be used to execute the job.  These files can then be used to create the resources by executing `kubectl create -f resources.yaml`.

To generate the resources file the `KubernetesJob` must have connectivity to Zookeeper and Kafka, so it can determine how many input partitions there are, so it can generate the job configuration a priori.  Like with `YarnJob`s, this means that the job must be shutdown and the config regenerated if the number of input partitions change.

`KubernetesJob` specifies one [Replication Controller](http://kubernetes.io/v1.1/docs/user-guide/replication-controller.html) with a replica count of one per `SamzaContainer`.  This ensures that if the node the `SamzaContainer` is executing fails the `SamzaContainer` will be recreated in a new node.

Within the container executing the `SamzaContainer` `/samza/state` is an `emptyDir` volume.  That means the Samza KV state is not lost from the local system is the `SamzaContainer` process fails.  It will be restarted on the same system and will be able to recover quickly as it local state won't be lost.

Each `SamzaContainer` executing within a Docker container is identified by its container id, which is passed to it via the `SAMZA_CONTAINER_ID` environment variable, just as with `YarnJob`s.

The configuration is passed in via the `SAMZA_JOB_CONFIG` environment variable. The config is just the serialized `JobModel`.  The `entrypoint.sh` helper script in the [samza_container_base](https://github.com/eliaslevy/docker-samza_container_base) Docker base image writes the config into a file and the places a file URL pointing to the file in the `SAMZA_COORDINATOR_URL` environment variable.  `SamzaContainer` uses this variable to fetch its configuration.

The job to execute must be packaged as a Docker image derived from the base image described above or one similar to it.

### Build

To build the project execute `sbt package`.  The output JAR file will be at `target/scala-2.10/samza_kubernetes_2.10-0.1.jar`.

### Package

To package your job follow the usual procedure.  You must then create a Docker image for your job.  To do so create a directory, place your job package archive in it, then create a `Dockerfile` like the following one, replacing `hello-samza-0.9.1-dist.tar.gz` with the name of your package:

```
FROM elevy/samza_container_base

COPY hello-samza-0.9.1-dist.tar.gz /
RUN tar -zxf hello-samza-0.9.1-dist.tar.gz -C /samza bin lib && \
        rm -f hello-samza-0.9.1-dist.tar.gz
```

Then build your image with `docker build -t <image_name> <dockerfile_dir>` and push the image to a repository accessible from your Kubernetes cluster.

### Job Config File

In your Samza job config properties file change the `job.factory` to `eliaslevy.samza.job.kubernetes.KubernetesJobFactory` and delete any YARN specific configuration properties.

`KubernetesJobs` introduces the following new properties:

- `kubernetes.container.image`: The Docker image for the job.
- `kubernetes.container.memory`: The memory limit of your job, expressed in the Kubernetes format (e.g. `64Mi` or `1Gi`).
- `kubernetes.container.cpu.cores`: The CPU units limit of your job, expressed in the Kubernetes format (e.g. `250m` or `1`).
- `kubernetes.pod.count`: The number of `SamzaContainer`s to create.
- `kubernetes.node.selector`: A [node selector](https://github.com/kubernetes/kubernetes/blob/release-1.1/docs/user-guide/node-selection/README.md) to target the job against specific nodes in the cluster. (Optional)
- `kubernetes.resources.out`: The file name to write the Kubernetes resources YAML to.

It is also recommended that you set the Java `-Xmx` option in the `task.opts` property to a value below the memory limit you set in `kubernetes.container.memory` so that your job is not killed by Kubernetes for exceeding its allocated memory limit.

Make sure `systems.kafka.consumer.zookeeper.connect` and `systems.kafka.producer.bootstrap.servers` and configured correctly to reach those services within the Kubernetes cluster.

### Generating the Kubernetes Config

To generate the config we must add the `samza_kubernetes` JAR file to our package.  Unarchive the job package, then add `samza_kubernetes_2.10-*.jar` file to the `lib` directory.  You must also add to the `lib` directory [`jackson-core-2.6.3.jar`](http://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/core/jackson-core/2.6.3/jackson-core-2.6.3.jar) and [`jackson-dataformat-yaml-2.6.3.jar`](http://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.6.3/jackson-dataformat-yaml-2.6.3.jar), which are dependencies of `samza_kubernetes`.  Then archive the package again.


Kubernetes has a very oppinionated networking model.  Services has virtual IPs that are not routable within the cluster. Each Pod has its own IP, which are commonly only reachable through overlay networking.  The DNS server executes as a Pod within the cluster.  All this makes it difficult to access cluster services from outside the cluster unless one setups up an external load balancer.

The solution to this is to execute a Pod within the cluster just to access cluster services for administrative purposes.  We can then access this Pod using `kubectl exec`.

To bootstart a `KubernetesJob` we create temporary pod using a spec like the following:

```
apiVersion: v1
kind: Pod
metadata:
  name: admin
spec:
  restartPolicy: OnFailure
  containers:
    - name: sh
      image: develar/java
      command:
        - sleep
        - "100000000"
```

We then obtain a shell on the pod by executing `kubectl exec -ti admin /bin/sh`.

You must then copy the package with the `samza_kubernetes` JARs and the job config properties file into the Pod.  You can place the files on some web server and download them into the Pod using something like `curl`.

Once the files are in place, you can execute your job to generate the Kubernetes YAML config.  Execute in the Pod `run-job.sh --config-path=<config_file_properties>`.  The Kubernetes config should now be in the location you specified in `kubernetes.resources.out`.  Save this file.  You may want to store it in a revision control system.

### Start your job in Kubernetes

To start your job in Kubernetes execute `kubectl create -f <kubernetes_job_yaml_config>`.


