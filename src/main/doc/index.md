# matomo-cf-service: user guide

This is user guide of the [Matomo](https://matomo.org/) service. As soon as it has been made available into a marketplace, it can be used as any OSB broker. For instance, on a [CloudFoundry](https://www.cloudfoundry.org/) (CF) platform, it supports all the functions of a broker: creating/deleting service instances, allowing application to bind/unbind to/from a service instance. The remainder of the document assumes a CloudFoundry platform through its examples.

Note that the actual releases proposed by the service can be found [here](releases.html). One of them may be explicitely specified at creation time.


## Create instances

Go to your CF marketplace and create a service instance by choosing among the proposed plans. There are three (only the first one is currently implemented, the two others are expected soon). Their objective is to provide different isolation levels in term of load n an instance, especially on the management of data (information from tracked Web sites):

1. global-shared-db

   Data of all instances are stored in a database platform mutualized with many others (useful for dev purpose).

2. matomo-shared-db (planned but not implemented yet)

   Data of all instances are stored in a database platform mutualized with all other Matomo service instances of this kind (useful for tracked Web sites with small traffic).

3. dedicated-db (planned but not implemented yet)

   Data of this Matomo service instance is stored in a dedicated database platform (useful for tracked Web sites with high traffic).

Indeed, the choice among them depends on the traffic of the tracked Web site and has an impact on the cost of the service instance.

While you have created a service instance, you can access it through the dashboard link associated to your instance. Indeed, to log into that Matomo instance, you need credentials. For that, you have to bind to that instance (see the following section).

## Bind to instances

### Why binding?

You can bind a Matomo service instance to an application, meaning that you want to track its usage. Meanwhile, binding creates a site for you within this Matomo instance as well as a user with "admin" role to manage it. All the required information to use this Matomo site is provided by the credential of the binding associated with the application concerned. Credentials contain:

* mcfs-siteId

This is the site id, assigned by Matomo at site creation (which occurs at binding time), as expected by the Matomo instance when it gets usage information (mainly from browsers). This information is produced by scripts that are added to Web pages, such script including the site id for identifying the Matomo site that you've setup to produce analysis.

* mcfs-userName

This is the user name of the admin of the created Matomo site. It is created at binding time as well.

* mcfs-password

This is the password of the admin user of the created Matomo site. It is created at binding time to.

Indeed both user name and password allow you to logged in the related Matomo instance (accessible through the dashboard associated to the service instance). You can then act as the admin of that site.

Example of the result of a binding setup with a CloudFoundry platform (i.e., service is consumed through a CF marketplace):

```
System-Provided:
{
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
