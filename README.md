# README #

A custom deployer for AXIS2 

### What is this repository for? ###

make the service that's running in WSO2(Axis2) could written in clojure.

### How to use it? ###

copy the generated jar file to axis2 server's lib dirtory

and setting conf/axis2.xml, by adding  the following line:

```
#!xml


  <deployer extension=".aar" directory="aar" class="com.gubnoi.Axis2Extender.ClojureDeployer"/>
```

that will make the file name with .aar extention , and in the axis2's repository directory's sub folder "aar", would be loaded as a service.

should restart the server to make it effective

### Contribution guidelines ###

* Writing tests
* Code review