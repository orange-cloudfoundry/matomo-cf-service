# User Guide

---

This is the user guide of the [Matomo](https://matomo.org/) service. As soon as it has been made available into a marketplace, it can be used as any OSB broker. For instance, on a [CloudFoundry](https://www.cloudfoundry.org/) (CF) platform, it supports all the functions of a broker: creating/deleting service instances, allowing application to bind/unbind to/from a service instance. The remainder of the document assumes a CloudFoundry platform through its examples.

Note that the actual releases proposed by the service can be found [here](releases.html) (this link is only active when accessing the documentation through a deployed service, else lead to 404). One of them may be explicitely specified at creation time. 


## Create instances

---

Go to your CF marketplace and create a service instance by choosing among the proposed plans. There are three (only the first one is currently implemented, the two others are expected soon). Their objective is to provide different isolation levels in term of load and security of an instance, especially on the management of data (information from tracked Web sites):

1. global-shared-db
   Data of all instances are stored in a database platform mutualized with many others (useful for dev purpose). Matomo is run by one container (application instance) within CloudFoundry.

2. matomo-shared-db
   Data of all instances are stored in a database platform mutualized with all other Matomo service instances of this kind (useful for tracked Web sites with small / medium traffic). Matomo is run by a cluster of two containers and is configured to run in cluster mode.

3. dedicated-db
   Data of this Matomo service instance is stored in a dedicated database platform (useful for tracked Web sites with high traffic). Matomo is run by a cluster of at least two containers and is configured to run in cluster mode. It may scale up two a ten containers cluster as requested by the owner of the service instance.

Indeed, the choice among them depends on the traffic of the tracked Web site and has an impact on the cost of the service instance.

When creating an instance, some parameters can be specified and particularly the release to deploy. Then the service instanciates that particular release as soon as it has been made available with the current service deployment. Here is an example of such a parameterized creation:
```
cf cs matomo-service global-shared-db m371 -c '{"matomoVersion": "3.7.1"}'
```

The time zone within which the instance executes can be specified the same way:
```
cf cs matomo-service global-shared-db m -c '{"matomoTimeZone": "Europe/Paris"}'
```

For the `dedicated-db` plan the number of containers that run the service instance can be specified (by default, it is two of them):
```
cf cs matomo-service dedicated-db md -c '{"matomoInstances": 3}'
```
It can be ajusted later on through an update action on the instance.

For the `matomo-shared-db` and the `dedicated-db` plans, the memory size (in MB) of the containers that run the service instance can be specified (by default, it is 512MB):
```
cf cs matomo-service matomo-shared-db ms -c '{"memorySize": 1024}'
```
Just like the number of running containers, it can be ajusted later on through an update action on the instance.

Service upgrade to new release (with higher version) is also supported by the service (see section "Update instance"). Concerning version upgrade, a policy can be specified at creation time:
```
cf cs matomo-service global-shared-db m -c '{"versionUpgradePolicy": "Explicit"}'
```

Version upgrade policy (see corresponding parameter name above) allows the created instance to behave differently when a newer version of Matomo is deployed within the service. There are two possibilities for this policy, the default one (i.e., parameter not specified) being `Automatic`:

* `Automatic`: This policy guarantees that this isntance is always at the latest level of Matomo release available. It assumes that if a new latest release of Matomo is available within the deployed Matomo CF service, as soon as the service starts again, all instances tagged this way are automatically upgraded to this new version.

* `Explicit`: This policy gives complete control on release management of Matomo instances to its owner. This means that no upgrade of release may happen until explicitly requested by the instance owner. This can only be done by requesting an update of this instance (see section "Update instance").

While you have created a service instance, you can access it through the dashboard link associated to your instance. Indeed, to log into that Matomo instance, you need credentials. For that, you have to bind to that instance (see section "Bind to instances").

In case the creation of your instance has failed, you can delete it and retry. Even if an operation is frozen "in progress", it will fail after 30 minutes elapse time in this status. Then it will be finally possible to delete it in the end.

## Update instances

---

Main instance update actions usually concentrates on changes of plan. This is not currently supported for this service as it requires database backup/restore. Furthermore, it is not straitforward that such a capability is a strong requirement for this service (to be discussed if needed). Thus, when updating an instance, some parameters need to be specified. Let's go through the different update possibilities using examples. First possibility is to upgrade an instance to a new Matomo release:
```
cf update-service m371 -c '{"matomoVersion": "3.10.0"}'
```

The second possibility is to change the version upgrade policy:
```
cf update-service m -c '{"versionUpgradePolicy": "Automatic"}'
```
Moving the policy to Automatic means that the instance will be forced to upgrade to the latest release right away.

The third possibility is to adjust the number of containers that run the instance (only in case of `dedicated-db plan`):
```
cf update-service m -c '{"matomoInstances": 6}'
```
This number is forced to stay in the interval [2..10]. This means that if a lower value than 2 is specified, then 2 is forced. In the same way, if a higher value than 10 is specified, then 10 is forced.

The fourth possibility is to adjust the memory of containers that run the instance (only in case of `matomo-shared-db` and `dedicated-db` plans):
```
cf update-service ms -c '{"memorySize": 768}'
```
This number is forced to stay in the interval [256..2048]. This means that if a lower value than 256 is specified, then 256 is forced. In the same way, if a higher value than 2048 is specified, then 2048 is forced.

## Bind to instances

---

### Why binding?

You can bind a Matomo service instance to an application, meaning that you want to track its usage. Meanwhile, binding creates a site for you within this Matomo instance as well as a user with "admin" role to manage it. Note that this Matomo site is not deleted when unbinding from a service instance. If one wants to remove it, it should be done through the Matomo management capabilities.

All the required information to use such a Matomo site is provided by the credential of the binding associated with the application concerned. Credentials contain:

* mcfs-matomoUrl
  This is the URL of the Matomo instance to be used when usage information are published (usually by browsers). It is defined within the script that is added to Web pages so that analytics information can be pushed to this Matomo instance.

* mcfs-siteId
  This is the site id, assigned by Matomo at site creation (which occurs at binding time), as expected by the Matomo instance when it gets usage information (mainly from browsers). This information is produced by the script that is added to Web pages, such script including the site id for identifying the Matomo site that you've setup to produce analysis.

* mcfs-userName
  This is the user name of the admin of the created Matomo site. It is created at binding time as well.
  
* mcfs-password
  This is the password of the admin user of the created Matomo site. It is created at binding time to.

Indeed both user name and password allow you to logged in the related Matomo instance (accessible through the dashboard associated to the service instance). You can then act as the admin of that site.

Example of the result of a binding setup with a CloudFoundry platform (i.e., service is consumed through a CF marketplace):

```
System-Provided: {
	"VCAP_SERVICES": {
		"matomo": [
			{
			"binding_name": null,
			"credentials": {
				"mcfs-password": "OJmfXGzaRZFq",
				"mcfs-siteId": 10,
				"mcfs-userName": "lsXdcB7f6xjG"
				},
			"instance_name": "m",
			"label": "matomo",
			"name": "m",
			"plan": "global-shared-db",
			"provider": null,
			"syslog_drain_url": null,
			"tags": [
				"Matomo",
				"web analytics"
				],
			"volume_mounts": []
			}
		],
	...
```

### Required parameters for binding

There are mandatory parameters that should be provided at binding time:

* siteName
  This is the name of the [Matomo site](https://matomo.org/docs/manage-websites/) that will be intanciated at binding to track the Web site associated with the application that binds to.

* trackedUrl
  This is the URL of the Web site to be tracked.

* adminEmail
  As the binding creates an "admin" user for managing the new Matomo site, you need also to provide the email address for that admin.

Example of a binding command showing the parameters to provide:

```
 cf bs matomo-service m -c '{
	"siteName": "mymatomosite",
	"trackedUrl": "http://www.mysite.com",
	"adminEmail": "john.smith@mycompany.com"
	}'
```
