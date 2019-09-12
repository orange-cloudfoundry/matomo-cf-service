# matomo-cf-service

This is a service broker (OSB compliant) that provides service instance as dedicated CF app.

## Requirements

## Installation

1. Clone this repository and position into the created _matomo-cf-release_ directory created.

2. Define the releases of Matomo you want provide to your service users. At least the latest stable one will always be proposed (e.g., matomo.zip). See [Matomo releases directory](https://builds.matomo.org/) to choose the ones you want.
   * Setup a file named `releases.txt` with a release name each line:
    ```
    3.6.1
    3.8.0-b1
    ```

   * Result

     Release files `matomo-3.6.1.zip` and `matomo-3.8.0-b1.zip` will be retrieved in the build phase of the service and packaged with it to be proposed for instanciation.

     If no file is defined, only the latest stable release of Matomo will be provided.

3. Build and package the service code to be deployed to CloudFoundry.
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

## How to use

### Create instances

Go to your CF marketplace and create a service instance by chossing among the proposed plans. There are three (only the first one is currently implemented, the two others are expected soon). Their objectives are to provide different isolation level for the load on the management of data (information from tracked Web sites):

1. global-shared-db

   Data of all instances are stored in a database platform mutualized with many others (useful for dev purpose).

2. matomo-shared-db

   Data of all instances are stored in a database platform mutualized with all other Matomo service instances of this kind (useful for tracked Web sites with small traffic).

3. dedicated-db

   Data of this Matomo service instance is stored in a dedicated database platform (useful for tracked Web sites with high traffic).

Indeed, the choice among them depends on the traffic of the tracked Wen site and has an impact on the cost of the service instance.

### Bind to instances