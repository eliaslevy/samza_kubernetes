lazy val root = (project in file(".")).
  settings(
    name 					:= "samza_kubernetes",
    version 			:= "0.1",
    scalaVersion 	:= "2.10.6",
    libraryDependencies ++= Seq(
  		"org.apache.samza" % "samza-core_2.10" % "0.9.1",
		  "com.fasterxml.jackson.core" 			 % "jackson-core" 					 % "2.6.3",
		  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.6.3"  		
		),
		scalacOptions ++= Seq(
			"-feature",
			"-language:implicitConversions"
		)
		// resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
  )