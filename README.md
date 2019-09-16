# matomo-cf-service

This is a service broker (OSB compliant) that provides [Matomo](https://matomo.org/) service instance as dedicated [CloudFoundry](https://www.cloudfoundry.org/) (CF) app. It requires CF for managing the service but can be used on any platform supporting the OSB API.

## How to use it

### Create instances

Go to your CF marketplace and create a service instance by chossing among the proposed plans. There are three (only the first one is currently implemented, the two others are expected soon). Their objectives are to provide different isolation level for the load on the management of data (information from tracked Web sites):

1. global-shared-db

   Data of all instances are stored in a database platform mutualized with many others (useful for dev purpose).

2. matomo-shared-db (planned but not implemented yet)

   Data of all instances are stored in a database platform mutualized with all other Matomo service instances of this kind (useful for tracked Web sites with small traffic).

3. dedicated-db (planned but not implemented yet)

   Data of this Matomo service instance is stored in a dedicated database platform (useful for tracked Web sites with high traffic).

Indeed, the choice among them depends on the traffic of the tracked Web site and has an impact on the cost of the service instance.

While you have created a service instance, you can access it through the dashboard link associated to your instance. Indeed, to log into that Matomo instance, you need credentials. For that, you have to bind to that instance (see the following section).

### Bind to instances

#### Why binding?

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

#### Required parameters for binding

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

## Installation

### Requirements

You need a CloudFoundry platform to install the Matomo service. You can install it globally so that it is available to all users of the platform or scoped to a space.
In any case, as the service and the instances it manages can be instanciated in different spaces, it is mandatory that the service can "ssh" its instances. Be careful that the security settings of your platform enable this communication.

For buidling Matomo service, you need a Unix-like environment such as Linux or Cygwin. Indeed, you need to be able to run shell scripts (sh or bash).

### Setup the service on a CloudFoundry platform

1. Clone this repository and position into the _matomo-cf-release_ directory that has been created.

2. Define the releases of Matomo you want provide to your service users. At least the latest stable one will always be proposed (e.g., matomo.zip). See [Matomo releases directory](https://builds.matomo.org/) to choose the ones you want.
   *  file named `releases.txt` with a release name each line:
    ```
    latest
    3.6.1
    3.8.0-b1
    ```

   * Result

     Release files `matomo-3.6.1.zip` and `matomo-3.8.0-b1.zip` will be retrieved in the build phase of the service and packaged with it to be proposed for instanciation.

     `latest` specify that the latest stable release of Matomo will be provided.

3. Build and package the service code to be deployed to CloudFoundry. Be patient, adapting Matomo versions may take a while (few minutes).
   * Build with maven:
   ```sh
   mvn clean install
   ```

   * Result

     It produces a jar file which contains the whole code for the service as well as the releases of Matomo to be proposed for service instanciation.

4. Deploy the service to CloudFoundry.
   * Configure your manifest file (for example copy the existing template `manifest.yml` to `mymanifest.yml`). The change the file to fit your context by replacing all $...$ strings accordingly:

   | $...$ String | Role | Example |
   |--------------|------|---------|
   | $YOUR_ROUTE$ | The route to which the Matomo service is exposed to | matomoserv.cf.mycompany.com |
   | $CF_API_HOST$ | The URL to call CloudFoundry API | cfapi.mycompany.com |
   | $USERNAME$ | The username to authenticate to the CF platform | dilbert@mycompany.org |
   | $PASSWORD$ | The password to complete authentication | helpme |
   | $CF_ORG$ | The CF org that contains the CF space for deployement | myorg |
   | $CF_SPACE$ | The CF space where service instance will be deployed | myspace |
   | $YOUR_DOMAIN$ | The domain whitin which service instances are exposed | matomo.mycompany.com |
   | $YOUR_SHARED_MYSQL_SERVICE$ | The name of the MySQL service from your CF marketplace | p-mysql |
   | $YOUR_SHARED_MYSQL_SERVICE_PLAN$ | The name of the plan form the previous MySQL service to be instanciated to manage service instance data | 100MB |
   | $YOUR_DEDICATED_MYSQL_SERVICE$ | The name of the MySQL service that provides dedicated database platform from your CF marketplace | my-mysql |
   | $YOUR_DEDICARED_MYSQL_SERVICE_PLAN$ | The name of the plan form the previous MySQL service to be instanciated to manage service instance data | 10GB |

   You can also adjust other parameters from that file, for instance, the maximum number of instances the Matomo service can create.

   * Deploy the Matomo service:
   ```
   cf push -f mymanifest.yml
   ```

   * Register the service to CF (example with space scoped for service testing):
   ```
    cf create-service-broker matomo-broker $USERNAME$ $PASSWORD$ $YOUR_ROUTE$ --space-scoped
   ```

5. You're done!!

### Register the broker

As soon as you have your Matomo service up and running, you have to register its associated broker to the marketplace with which you will consume it. Going on playing with a CF platform, you can register it in the scope of your own space for testing purpose:

```
cf create-service-broker matomo-broker yourusername yourpassword matomoserv.cf.mycompany.com --space-scoped
```

Then the service should be available in your CF marketplace and you can start envoy with your Matomo instances. For the time being, the only available actions are create/delete service instances and bind/unbind a service instance to an application.