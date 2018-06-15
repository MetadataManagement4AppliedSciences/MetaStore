# MetaStore Service

The MetaStore service extends the core KIT DM by the following features

* Registration of XML Schema
* Validation of XML with registered XML Schema
* Storing metadata (METS format)
* Updating metadata
* Searching for metadata via the Generic Repository Client of the KIT DM or the MetaStore REST-API
* Publish a digital object (create a PID)
* Providing a REST-API for the MetaStore functionality that is fully integrated with the KIT DM REST-API

## How to build

In order to build the MetaStore Service you'll need:

* Java SE Development Kit 8 or higher
* Apache Maven 3

Change to the folder where the sources are located, e.g.: /home/user/metastore/. 
Afterwards, just call:

```
user@localhost:/home/user/metastore/$ bash buildMetaStore.sh
[...]
[INFO] Building zip: /home/user/metastore/MetaStoreService/zip/MetaStoreService-1.3-kitdm-kitdm.zip
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 10.012 s
[INFO] Finished at: 2017-09-22T11:12:14+02:00
[INFO] Final Memory: 74M/1286M
[INFO] ------------------------------------------------------------------------
user@localhost:/home/user/metastore/$
```

As soon as the assembly process has finished there will be a file named `MetaStoreService-1.3-kitdm-kitdm.zip` located at /home/user/metastore/zip, which is the distribution package of the client containing everything you need to launch the tool. Extract the zip file to a directory of your choice and refer to the contained manual for further instructions.

## More Information

* [REST API](http://ipelsdf1.lsdf.kit.edu/masi/MetaStore/swagger/)
* [Bugtracker](http://datamanager.kit.edu/bugtracker/thebuggenie/)

## License

The MetaStore Service is licensed under the Apache License, Version 2.0.


