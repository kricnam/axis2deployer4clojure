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



### Contribution guidelines ###

* Writing tests
* Code review
