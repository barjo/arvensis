#RoSe Readme 


## Source Organization: 

This folder contains the code source of the OW2 Chameleon RoSe project.

  - _core_: This project contains the RoSe API and core component.  
  - _jaxrs_: This project contains the RoSe components working with the jax-rs API.
  - _jaxws_: This project contains the RoSe components working with the jax-ws API.
  - _jsonrpc_: This project contains the RoSe components working with the json-rpc protocol.
  - _machines_: This project contains several RoSe distributions. 
  - _registry_: This project contains the RoSe networked registry component implementation.
  - _testing_: This project contains the RoSe testing helpers.  

## License

RoSe is licensed under the Apache License 2.0.


## Infrastructure

### Repository 
		
```xml
<repository>
	<id>maven-barjo-repository-release</id>
	<name>RoSe - Release</name>
	<url>http://repository-barjo.forge.cloudbees.com/release/</url>
	<layout>default</layout>
</repository>
<repository>
	<id>maven-barjo-repository-snapshot</id>
	<name>RoSe - Snapshot</name>
	<url>http://repository-barjo.forge.cloudbees.com/snapshot/</url>
	<layout>default</layout>
</repository>
```

#Released Version semantic: 

 major.minor.revision 

 * _major_ changed when there are modification or addition in the functionalities. 
 * _minor_ changed when minor features or critical fixes have been added.
 * _revision_ changed when minor bugs are fixed.


