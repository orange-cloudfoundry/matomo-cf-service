# matomo-cf-service

This is a service broker (OSB compliant) that provides [Matomo](https://matomo.org/) service instance as dedicated [CloudFoundry](https://www.cloudfoundry.org/) (CF) applications. It requires CF for managing the service but can be used on any platform supporting the OSB API.

The user guide of the service is available [here](src/main/doc/index.md).

## Tributes
The scripting code to adapt Matomo configuration code to CloudFoundry is largely inspired from material available from previous experiments and especially from [Sanjay Joshi](https://www.ibm.com/blogs/cloud-archive/2014/07/getting-started-piwik-ibm-bluemix/) at IBM as well as from [Bespinian blog](https://blog.bespinian.io/posts/run-piwik-on-cloud-foundry/). Many thanks to them.

## Requirements

You need a CloudFoundry platform to install the Matomo service. You can install it globally so that it is available to all users of the platform or scoped to a space.
In any case, as the service and the instances it manages can be provisioned in different spaces, it is mandatory that the service can "ssh" its instances. Be careful that the security settings of your platform enable this communication.

For buidling Matomo service, you need a Unix-like environment such as Linux or Cygwin. Indeed, you need to be able to run shell scripts (sh or bash).

The service has been tested with Matomo versions starting from 3.6 until last version 3.12.

## Installation

### Managing Matomo releases

The service supports to deal with several releases of Matomo when creating instances. Two files present at the root of the repository allow the configuration of the releases to be proposed for usage as well the default one to be used (if the release is not specified at creation time).

All available releases of Matomo can be found in [Matomo releases directory](https://builds.matomo.org/). Select the ones you want from there by listing them as follows:
*  The file `releases.txt` contains all the selected releases, giving one release name each line:
```
latest
3.6.1
3.8.0-b1
```

* Result

     Release files `matomo.zip`, `matomo-3.6.1.zip` and `matomo-3.8.0-b1.zip` will be retrieved in the build phase of the service and packaged with it to be proposed for instanciation.

     `latest` specify that the latest stable release of Matomo has been selected, which corresponds to release file `matomo.zip`.

Then the default release can also be selected as follows:
*  The file `default-release.txt` contains the release name that has been chosen:
```
3.6.1
```

* Result

  Release `3.6.1` is the chosen one to be instanciated when none is specified at instance creation time.

If none of `releases.txt` and `default-release.txt` is found at repository root, then by default, only the latest release is proposed for usage and is thus the default and unique one!

### Setup the service on a CloudFoundry platform

1. Clone this repository and position into the _matomo-cf-release_ directory that has been created.

2. Define the releases of Matomo you want provide to your service users. Setup files `releases.txt` and `default-release.txt` accordingly.

3. Build and package the service code to be deployed to CloudFoundry. Be patient, adapting Matomo versions may take a while (few minutes).
   * Build with maven:
   ```sh
   mvn clean install
   ```

   * Result

     It produces a jar file which contains the whole code for the service as well as the releases of Matomo to be proposed for service instanciation.

4. Deploy the service to CloudFoundry.
   * Configure your manifest file (for example copy the existing template `manifest.yml` to `mymanifest.yml`). This file has the following form:

```
applications:
- name: matomo-service
  memory: 1G
  instances: 1
  routes:
    - route: $YOUR_ROUTE$
  path: target/matomo-cf-service-0.1.0.jar
  buildpacks:
    - java_buildpack
  services:
    - matomo-service-db
  env:
    CF_APIHOST: $CF_API_HOST$
    CF_USERNAME: $USERNAME$
    CF_PASSWORD: $PASSWORD$
    CF_ORGANIZATION: $CF_ORG$
    CF_SPACE: $CF_SPACE$
    MATOMO-SERVICE_SECURITY_ADMINNAME: $ADMIN_NAME$
    MATOMO-SERVICE_SECURITY_ADMINPASSWORD: $ADMIN_PASSWORD$
    MATOMO-SERVICE_SECURITY_ADMINSESSIONTIMEOUT: $ADMIN_SESSION_TIMEOUT$
    MATOMO-SERVICE_CONTACT_NAME: $CONTACT_NAME$
    MATOMO-SERVICE_CONTACT_URL: $CONTACT_URL$
    MATOMO-SERVICE_CONTACT_EMAIL: $CONTACT_EMAIL$
    MATOMO-SERVICE_MAX-SERVICE-INSTANCES: $MAX_INSTANCES$
    MATOMO-SERVICE_SMTP_CREDS: $YOUR_SMTP_SERVICE_CREDS$
    MATOMO-SERVICE_SMTP_CREDS: $SMTP_SERVICE_CREDS$
    MATOMO-SERVICE_DOMAIN: $DOMAIN$
    MATOMO-SERVICE_PHPBUILDPACK: $PHP_BUILPACK$
    MATOMO-SERVICE_SHARED-DB_CREDS: $GLOBAL_SHARED_MYSQL_SERV_CREDS$
    MATOMO-SERVICE_MATOMO-SHARED-DB_CREDS: $MATOMO_SHARED_MYSQL_SERV_CREDS$
    MATOMO-SERVICE_DEDICATED-DB_CREDS: $DEDICATED_MYSQL_SERV_CREDS$
  timeout: 180
```

   Change the file to fit your context by replacing all $...$ strings accordingly:

   | $...$ String | Role | Example |
   |--------------|------|---------|
   | $YOUR_ROUTE$ | The route to which the Matomo service is exposed to | matomoserv.cf.mycompany.com |
   | $CF_API_HOST$ | The URL to call CloudFoundry API | cfapi.mycompany.com |
   | $USERNAME$ | The username to authenticate to the CF platform | dilbert@mycompany.org |
   | $PASSWORD$ | The password to complete authentication | helpme |
   | $CF_ORG$ | The CF org that contains the CF space for deployement | myorg |
   | $CF_SPACE$ | The CF space where service instance will be deployed | myspace |
   | $ADMIN_NAME$ | The name of the admin user to be used for authentication before accessing the admin API | scott |
   | $ADMIN_PASSWORD$ | The password of the admin user | tiger |
   | $ADMIN_SESSIONTIMEOUT$ | The timeout in minutes for admin session (default is 15) | 30 |
   | $CONTACT_NAME$ | The contact name of the responsible entity for the admin API | API Provider |
   | $CONTACT_URL$ | The contact URL of the responsible entity for the admin API | https://github.com/orange-cloudfoundry/matomo-cf-service |
   | $CONTACT_EMAIL$ | The contact URL of the responsible entity for the admin API | dilbert@mycompany.org |
   | $MAX_INSTANCES$ | The maximum number of instances that can be created by the service (note that it can be increased when redeploying) | 100 |
   | $SMTP_SERVICE_CREDS$ | This is a column-separated string which mainly consists of the names of the credential fields in VCAP_SERVICES for the SMTP service to be bound to in order to send e-mails from Matomo instances | o-smtp:smtp-prod:host:port:username:password |
   | $DOMAIN$ | The domain within which service instances are exposed | matomo.mycompany.com |
   | $PHP_BUILDPACK$ | The PHP buildpack to be used to push Matomo instance to CF | php_buildpack |
   | $GLOBAL_SHARED_MYSQL_SERV_CREDS$ | This is a column-separated string which mainly consists of the names of the service and plan as well as the names of the credential fields in VCAP_SERVICES for the MySQL/MariaDB service to be bound to in order to store data for Matomo instances within a globally shared DB |  p-mysql:100MB:name:hostname:port:username:password |
   | $MATOMO_SHARED_MYSQL_SERV_CREDS$ | This is a column-separated string which mainly consists of the names of the service and plan as well as the names of the credential fields in VCAP_SERVICES for the MySQL/MariaDB service to be bound to in order to store data for Matomo instances within a shared DB exclusively associated with Matomo service |  p-mysql:1GB:name:hostname:port:username:password |
   | $DEDICARED_MYSQL_SERV_CREDS$ | This is a column-separated string which mainly consists of the names of the credential fields in VCAP_SERVICES for the MySQL/MariaDB service to be bound to in order to store data for Matomo instances within a dedicated DB | ded-mysql:10GB:name:hostname:port:username:password |

   You can also adjust other parameters from that file, for instance, the maximum number of instances the Matomo service can create.

   * Deploy the Matomo service:
   ```
   cf push -f mymanifest.yml
   ```

5. You're done!!

### Register the broker

As soon as you have your Matomo service up and running, you have to register its associated broker to the marketplace with which you will consume it. Going on playing with a CF platform, you can register it in the scope of your own space for testing purpose:

```
cf create-service-broker matomo-broker $ADMIN_NAME$ $ADMIN_PASSWORD$ $YOUR_ROUTE$ --space-scoped
```

Then the service should be available in your CF marketplace and you can start envoy with your Matomo instances. For the time being, the only available actions are create/delete service instances and bind/unbind a service instance to an application.

## Test the service

Two examples are provided in order to test the service and especially the way to bind to service instances. The first one is a [simple static Web site](examples/static-web) and the second one is a [Java-based dynamic Web application](examples/java-web). As soon as your Matomo CF service is up and running, let's play with these examples to validate the expected behaviour.