//
// Copyright 2015 eleflow.com.br.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import sbt._
import sbt.Keys._
import sbt.complete.Parsers
import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._
import com.typesafe.sbt.SbtGit._

versionWithGit

organization := "eleflow"

name := "IUberdataCore"

version :="0.1.0"

resolvers += Resolver.sonatypeRepo("public")

resolvers += Resolver.typesafeIvyRepo("releases")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.mavenLocal

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases"

testOptions in Test += Tests.Argument("-oDF")

parallelExecution in Test := false

net.virtualvoid.sbt.graph.Plugin.graphSettings

def iUberdataCoreVersion(version:Option[String] = Some("Not a Git Repository"), dir:File) = {
  val file = dir / "UberdataCoreVersion.scala"
  IO.write(file,
    s"""package eleflow.uberdata.core\n  object UberdataCoreVersion{\n          val version = "${version.get}"\n
       |}\n""".stripMargin)
  Seq(file)
}

sourceGenerators in Compile <+= (git.gitHeadCommit,sourceManaged in Compile) map iUberdataCoreVersion
